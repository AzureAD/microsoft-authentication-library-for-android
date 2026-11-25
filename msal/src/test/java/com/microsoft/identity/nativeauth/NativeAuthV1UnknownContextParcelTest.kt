// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.nativeauth

import android.os.Parcel
import android.os.Parcelable
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.e2e.shadows.ShadowAndroidSdkStorageEncryptionManager
import com.microsoft.identity.client.e2e.tests.PublicClientApplicationAbstractTest
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.nativeauth.parameters.NativeAuthChallengeAuthMethodParameters
import com.microsoft.identity.nativeauth.statemachine.errors.Error
import com.microsoft.identity.nativeauth.statemachine.results.RegisterStrongAuthSubmitChallengeResult
import com.microsoft.identity.nativeauth.statemachine.states.AwaitingMFAState
import com.microsoft.identity.nativeauth.statemachine.states.MFARequiredState
import com.microsoft.identity.nativeauth.statemachine.states.NativeAuthV1RequestContext
import com.microsoft.identity.nativeauth.statemachine.states.RegisterStrongAuthState
import com.microsoft.identity.nativeauth.statemachine.states.RegisterStrongAuthVerificationRequiredState
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowAndroidSdkStorageEncryptionManager::class])
class NativeAuthV1UnknownContextParcelTest : PublicClientApplicationAbstractTest() {
    private lateinit var config: NativeAuthPublicClientApplicationConfiguration
    private val token = "test-continuation-token"
    private val correlation = "test-correlation"
    private val scopes = listOf("scope")
    private val claims = """{"id_token":{"xms_agvs":{"additionalProperties":["target-mc"]}}}"""
    private val sentinel = "adjacent-parcel-field"
    private val method = AuthMethod("email", "oob", null, "email")
    private val challengeParameters = NativeAuthChallengeAuthMethodParameters(method, "user@example.com")

    private enum class Kind { AWAITING_MFA, MFA, REGISTER, REGISTER_VERIFICATION }

    override fun getConfigFilePath() = "src/test/res/raw/native_auth_native_only_test_config.json"

    @Before
    override fun setup() {
        val application = PublicClientApplication.createNativeAuthPublicClientApplication(
            ApplicationProvider.getApplicationContext(), File(getConfigFilePath()))
        config = NativeAuthPublicClientApplication::class.java.getDeclaredField("nativeAuthConfig").let {
            it.isAccessible = true
            it.get(application) as NativeAuthPublicClientApplicationConfiguration
        }
        CommandDispatcher.getSilentExecutorPoolSize()
        mockkStatic(CommandDispatcher::class)
        every { CommandDispatcher.submitSilentReturningFuture(any()) } throws
            AssertionError("Unknown request context must not dispatch")
    }

    @After
    fun tearDown() {
        unmockkStatic(CommandDispatcher::class)
    }

    @Test
    fun legacyWriterFixturesRejectEveryOperationAndPreserveAdjacentField() = runTest(timeout = 60.seconds) {
        for (kind in Kind.values()) {
            val restored = restore(kind, bytes { writeLegacy(kind, it) })
            assertUnknownOperations(restored)
        }
        verify(exactly = 0) { CommandDispatcher.submitSilentReturningFuture(any()) }
    }

    @Test
    fun newNamedAndKnownEmptyStatesKeepLegacyBytesAndBecomeUnknownAfterRestore() = runTest(timeout = 60.seconds) {
        for (requestedClaims in listOf(claims, null)) {
            val context = NativeAuthV1RequestContext(requestedClaims)
            val states: Map<Kind, Parcelable> = linkedMapOf(
                Kind.AWAITING_MFA to AwaitingMFAState(token, correlation, scopes, config, context),
                Kind.MFA to MFARequiredState(token, correlation, scopes, config, context),
                Kind.REGISTER to RegisterStrongAuthState(token, correlation, config, context),
                Kind.REGISTER_VERIFICATION to RegisterStrongAuthVerificationRequiredState(token, correlation, config, context)
            )
            for ((kind, state) in states) {
                val encoded = bytes { state.writeToParcel(it, 0) }
                assertArrayEquals(bytes { writeLegacy(kind, it) }, encoded)
                assertUnknownOperations(restore(kind, encoded))
            }
        }
        verify(exactly = 0) { CommandDispatcher.submitSilentReturningFuture(any()) }
    }

    @Test
    fun restoredJitCallbackReportsRestartOnceWithoutDispatch() = runTest(timeout = 60.seconds) {
        val state = restore(Kind.REGISTER_VERIFICATION,
            bytes { writeLegacy(Kind.REGISTER_VERIFICATION, it) }) as RegisterStrongAuthVerificationRequiredState
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val applicationScope = NativeAuthPublicClientApplication.pcaScope
        mockkObject(applicationScope)
        try {
            every { applicationScope.coroutineContext } returns scope.coroutineContext
            val completed = CompletableDeferred<RegisterStrongAuthSubmitChallengeResult>()
            var callbacks = 0
            state.submitChallenge("123456",
                object : RegisterStrongAuthVerificationRequiredState.SubmitChallengeCallback {
                    override fun onResult(result: RegisterStrongAuthSubmitChallengeResult) {
                        callbacks++
                        completed.complete(result)
                    }
                    override fun onError(exception: BaseException) {
                        completed.completeExceptionally(exception)
                    }
                })
            assertRestart(completed.await() as Error)
            assertEquals(1, callbacks)
            verify(exactly = 0) { CommandDispatcher.submitSilentReturningFuture(any()) }
        } finally {
            scope.cancel()
            unmockkObject(applicationScope)
        }
    }

    private suspend fun assertUnknownOperations(state: Parcelable) {
        val results: List<Any> = when (state) {
            is AwaitingMFAState -> listOf(state.requestChallenge(method))
            is MFARequiredState -> listOf(state.requestChallenge(method), state.submitChallenge("123456"))
            is RegisterStrongAuthState -> listOf(state.challengeAuthMethod(challengeParameters))
            is RegisterStrongAuthVerificationRequiredState -> listOf(
                state.challengeAuthMethod(challengeParameters), state.submitChallenge("123456"))
            else -> throw AssertionError("Unexpected state")
        }
        results.forEach { assertRestart(it as Error) }
    }

    private fun assertRestart(error: Error) {
        assertEquals("invalid_state", error.error)
        assertEquals(correlation, error.correlationId)
        assertTrue(error.errorMessage!!.contains("restart sign-in"))
        assertFalse(error.errorMessage!!.contains(token))
        assertFalse(error.errorMessage!!.contains(claims))
    }

    // Literal existing writer layouts, independent of the production writers under test.
    private fun writeLegacy(kind: Kind, parcel: Parcel) {
        parcel.writeString(token)
        parcel.writeString(correlation)
        if (kind == Kind.AWAITING_MFA || kind == Kind.MFA) parcel.writeStringList(scopes)
        parcel.writeSerializable(config)
    }

    private fun bytes(write: (Parcel) -> Unit): ByteArray {
        val parcel = Parcel.obtain()
        try {
            write(parcel)
            parcel.writeString(sentinel)
            return parcel.marshall()
        } finally {
            parcel.recycle()
        }
    }

    private fun restore(kind: Kind, encoded: ByteArray): Parcelable {
        val parcel = Parcel.obtain()
        try {
            parcel.unmarshall(encoded, 0, encoded.size)
            parcel.setDataPosition(0)
            val state: Parcelable = when (kind) {
                Kind.AWAITING_MFA -> AwaitingMFAState.CREATOR.createFromParcel(parcel)
                Kind.MFA -> MFARequiredState.CREATOR.createFromParcel(parcel)
                Kind.REGISTER -> RegisterStrongAuthState.CREATOR.createFromParcel(parcel)
                Kind.REGISTER_VERIFICATION -> RegisterStrongAuthVerificationRequiredState.CREATOR.createFromParcel(parcel)
            }
            assertEquals(sentinel, parcel.readString())
            assertEquals(0, parcel.dataAvail())
            return state
        } finally {
            parcel.recycle()
        }
    }
}
