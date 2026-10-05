//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.nativeauth.v2

import android.os.Parcel
import android.os.Parcelable
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationStateTestFactory
import com.microsoft.identity.common.java.util.ResultFuture
import com.microsoft.identity.nativeauth.AuthMethod
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.UserAttributes
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.statemachine.states.AttributesInvalidStateV2
import com.microsoft.identity.nativeauth.statemachine.states.AttributesRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.BaseState
import com.microsoft.identity.nativeauth.statemachine.states.CodeRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.MFARequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.MFAVerificationRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.NativeAuthBaseStateV2
import com.microsoft.identity.nativeauth.statemachine.states.NewPasswordRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.PasswordRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordMethodRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.SignInAfterResetPasswordStateV2
import com.microsoft.identity.nativeauth.statemachine.states.SignInAfterSignUpStateV2
import com.microsoft.identity.nativeauth.statemachine.states.SignInCodeRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.StrongAuthRegistrationRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.StrongAuthVerificationRequiredStateV2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/**
 * Unit tests for the Native Auth V2 states, covering Parcelable serialization and the
 * callback-based method overloads (which currently return the "not implemented" error).
 */
@RunWith(RobolectricTestRunner::class)
class NativeAuthV2StatesTest {

    private val correlationId = "correlation-id"
    private val scenario = NativeAuthFlowScenarioV2.SIGN_IN
    private val config = NativeAuthPublicClientApplicationConfiguration().apply {
        dc = "parcel-dc"
        setChallengeTypes(listOf("password", "oob"))
    }

    @Test
    fun v2StatesDoNotInheritTheV1ContinuationTokenContract() {
        assertFalse(BaseState::class.java.isAssignableFrom(NativeAuthBaseStateV2::class.java))
        var type: Class<*>? = NativeAuthBaseStateV2::class.java
        while (type != null) {
            assertFalse(type.declaredFields.any { it.name == "continuationToken" })
            assertFalse(type.declaredMethods.any { it.name.startsWith("getContinuationToken") })
            type = type.superclass
        }
    }

    @Test
    fun v2ParcelStartsWithCorrelationIdRatherThanARawTokenPlaceholder() {
        val state = CodeRequiredStateV2(
            createContinuationState(),
            NativeAuthFlowScenarioV2.RESET_PASSWORD,
            config
        )
        val parcel = Parcel.obtain()
        try {
            state.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            assertEquals(correlationId, parcel.readString())
            assertEquals(NativeAuthFlowScenarioV2.RESET_PASSWORD.name, parcel.readString())
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun v1StateRetainsRawTokenContractAndParcelPrefix() {
        val state = SignInCodeRequiredState(
            "v1-continuation-token",
            correlationId,
            "user@example.com",
            listOf("scope"),
            null,
            config
        )
        val baseState: BaseState = state
        assertEquals("v1-continuation-token", baseState.continuationToken)
        assertEquals(correlationId, baseState.correlationId)
        val parcel = Parcel.obtain()
        val restoredParcel = Parcel.obtain()
        try {
            state.writeToParcel(parcel, 0)
            val bytes = parcel.marshall()
            restoredParcel.unmarshall(bytes, 0, bytes.size)
            restoredParcel.setDataPosition(0)
            assertEquals("v1-continuation-token", restoredParcel.readString())
            assertEquals(correlationId, restoredParcel.readString())
        } finally {
            parcel.recycle()
            restoredParcel.recycle()
        }
    }

    private fun <T : NativeAuthBaseStateV2> assertParcelRoundTrip(
        state: T,
        creator: Parcelable.Creator<T>
    ): T {
        assertEquals(0, state.describeContents())
        val parcel = Parcel.obtain()
        val restoredParcel = Parcel.obtain()
        try {
            state.writeToParcel(parcel, 0)
            val bytes = parcel.marshall()
            restoredParcel.unmarshall(bytes, 0, bytes.size)
            restoredParcel.setDataPosition(0)
            val restored = creator.createFromParcel(restoredParcel)
            assertEquals(state.correlationId, restored.correlationId)
            assertEquals(state.scenario, restored.scenario)
            assertEquals(state.config.dc, restored.config.dc)
            assertEquals(state.config.getChallengeTypes(), restored.config.getChallengeTypes())
            assertEquals(restoredParcel.dataSize(), restoredParcel.dataPosition())
            assertEquals(1, creator.newArray(1).size)
            return restored
        } finally {
            parcel.recycle()
            restoredParcel.recycle()
        }
    }

    @Test
    fun testStatesAreParcelable() {
        assertParcelRoundTrip(
            CodeRequiredStateV2(correlationId, scenario, config),
            CodeRequiredStateV2.CREATOR
        )
        assertParcelRoundTrip(
            PasswordRequiredStateV2(correlationId, scenario, config),
            PasswordRequiredStateV2.CREATOR
        )
        assertParcelRoundTrip(
            NewPasswordRequiredStateV2(correlationId, scenario, config),
            NewPasswordRequiredStateV2.CREATOR
        )
        assertParcelRoundTrip(
            AttributesRequiredStateV2(correlationId, scenario, config),
            AttributesRequiredStateV2.CREATOR
        )
        assertParcelRoundTrip(
            AttributesInvalidStateV2(correlationId, scenario, config),
            AttributesInvalidStateV2.CREATOR
        )
        assertParcelRoundTrip(
            MFARequiredStateV2(correlationId, scenario, config),
            MFARequiredStateV2.CREATOR
        )
        val restoredResetPasswordMethodState = assertParcelRoundTrip(
            ResetPasswordMethodRequiredStateV2(
                correlationId,
                NativeAuthFlowScenarioV2.RESET_PASSWORD,
                config,
                authMethods = listOf(AuthMethod("sms-1", "sms", "+X XXX XXX 34", "sms"))
            ),
            ResetPasswordMethodRequiredStateV2.CREATOR
        )
        assertEquals("sms-1", restoredResetPasswordMethodState.authMethods.single().id)
        assertParcelRoundTrip(
            MFAVerificationRequiredStateV2(correlationId, scenario, config),
            MFAVerificationRequiredStateV2.CREATOR
        )
        assertParcelRoundTrip(
            StrongAuthRegistrationRequiredStateV2(correlationId, scenario, config),
            StrongAuthRegistrationRequiredStateV2.CREATOR
        )
        assertParcelRoundTrip(
            StrongAuthVerificationRequiredStateV2(correlationId, scenario, config),
            StrongAuthVerificationRequiredStateV2.CREATOR
        )
        assertParcelRoundTrip(
            SignInAfterResetPasswordStateV2(correlationId, scenario, config),
            SignInAfterResetPasswordStateV2.CREATOR
        )
        assertParcelRoundTrip(
            SignInAfterSignUpStateV2(correlationId, scenario, config),
            SignInAfterSignUpStateV2.CREATOR
        )
    }

    @Test
    fun testContinuationStatesParcelOpaqueState() {
        val continuationState = createContinuationState()

        val restoredCodeState = assertParcelRoundTrip(
            CodeRequiredStateV2(continuationState, NativeAuthFlowScenarioV2.RESET_PASSWORD, config),
            CodeRequiredStateV2.CREATOR
        )
        val restoredPasswordState = assertParcelRoundTrip(
            NewPasswordRequiredStateV2(continuationState, NativeAuthFlowScenarioV2.RESET_PASSWORD, config),
            NewPasswordRequiredStateV2.CREATOR
        )
        val restoredSignInState = assertParcelRoundTrip(
            SignInAfterResetPasswordStateV2(continuationState, NativeAuthFlowScenarioV2.RESET_PASSWORD, config),
            SignInAfterResetPasswordStateV2.CREATOR
        )

        val restoredStates = listOf(
            restoredCodeState,
            restoredPasswordState,
            restoredSignInState,
            assertParcelRoundTrip(
                PasswordRequiredStateV2(continuationState, scenario, config),
                PasswordRequiredStateV2.CREATOR
            ),
            assertParcelRoundTrip(
                AttributesRequiredStateV2(continuationState, NativeAuthFlowScenarioV2.SIGN_UP, config),
                AttributesRequiredStateV2.CREATOR
            ),
            assertParcelRoundTrip(
                AttributesInvalidStateV2(continuationState, NativeAuthFlowScenarioV2.SIGN_UP, config),
                AttributesInvalidStateV2.CREATOR
            ),
            assertParcelRoundTrip(
                SignInAfterSignUpStateV2(continuationState, NativeAuthFlowScenarioV2.SIGN_UP, config),
                SignInAfterSignUpStateV2.CREATOR
            ),
            assertParcelRoundTrip(
                MFAVerificationRequiredStateV2(continuationState, scenario, config),
                MFAVerificationRequiredStateV2.CREATOR
            )
        )
        restoredStates.forEach { restored ->
            assertEquals(correlationId, restored.correlationId)
            assertEquals(listOf("scope"), restored.continuationState?.scopesForTokenRequest())
            assertEquals("NativeAuthV2ContinuationState(<redacted>)", restored.continuationState.toString())
        }

        val methods = listOf(AuthMethod("sms-1", "sms", "+X XXX XXX 34", "sms"))
        val restoredMfa = assertParcelRoundTrip(
            MFARequiredStateV2(continuationState, methods, scenario, config),
            MFARequiredStateV2.CREATOR
        )
        val restoredResetPasswordMethods = assertParcelRoundTrip(
            ResetPasswordMethodRequiredStateV2(continuationState, methods, config),
            ResetPasswordMethodRequiredStateV2.CREATOR
        )
        listOf(restoredMfa, restoredResetPasswordMethods).forEach { restored ->
            assertEquals(listOf("scope"), restored.continuationState?.scopesForTokenRequest())
        }
        assertEquals(methods, restoredMfa.authMethods)
        assertEquals(methods, restoredResetPasswordMethods.authMethods)
    }

    private fun createContinuationState(): NativeAuthV2ContinuationState =
        NativeAuthV2ContinuationStateTestFactory.create(correlationId)

    private fun assertCallbackNotImplemented(action: (ResultFuture<NativeAuthResultV2>) -> Unit) {
        val future = ResultFuture<NativeAuthResultV2>()
        action(future)
        val result = future.get(30, TimeUnit.SECONDS)
        assertTrue(result is NativeAuthErrorV2)
        assertTrue((result as NativeAuthErrorV2).isNotImplemented())
        assertEquals(scenario, result.scenario)
    }

    private fun assertCallbackInvalidState(action: (ResultFuture<NativeAuthResultV2>) -> Unit) {
        val future = ResultFuture<NativeAuthResultV2>()
        action(future)
        val result = future.get(30, TimeUnit.SECONDS)
        assertTrue(result is NativeAuthErrorV2)
        assertEquals(ErrorTypes.INVALID_STATE, (result as NativeAuthErrorV2).errorType)
        assertEquals("The continuation state is unavailable. Restart the flow.", result.errorMessage)
        assertEquals(scenario, result.scenario)
    }

    /**
     * Drives the error path of the callback overloads: when result delivery throws an
     * [MsalException], the state's `catch` block must route it to the callback's `onError`. This
     * covers the `catch`/`onError` branch of each callback overload, complementing the success-path
     * tests above.
     */
    private fun assertCallbackRoutesToOnError(action: (ResultFuture<NativeAuthResultV2>, MsalException) -> Unit) {
        val future = ResultFuture<NativeAuthResultV2>()
        val thrown = MsalClientException("test_error", "boom")
        action(future, thrown)
        try {
            future.get(30, TimeUnit.SECONDS)
            fail("Expected the exception to be routed to onError")
        } catch (e: ExecutionException) {
            assertSame(thrown, e.cause)
        }
    }

    @Test
    fun testCodeRequiredStateCallbacksReturnInvalidState() {
        assertCallbackInvalidState { future ->
            CodeRequiredStateV2(correlationId, scenario, config).submitCode(
                "1234",
                object : CodeRequiredStateV2.SubmitCodeCallback {
                    override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        assertCallbackInvalidState { future ->
            CodeRequiredStateV2(correlationId, scenario, config).resendCode(
                object : CodeRequiredStateV2.ResendCodeCallback {
                    override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
    }

    @Test
    fun testPasswordRequiredStateCallbackReturnsInvalidState() {
        assertCallbackInvalidState { future ->
            PasswordRequiredStateV2(correlationId, scenario, config).submitPassword(
                "password".toCharArray(),
                object : PasswordRequiredStateV2.SubmitPasswordCallback {
                    override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
    }

    @Test
    fun testNewPasswordRequiredStateCallbackReturnsNotImplemented() {
        assertCallbackNotImplemented { future ->
            NewPasswordRequiredStateV2(correlationId, scenario, config).submitNewPassword(
                "password".toCharArray(),
                object : NewPasswordRequiredStateV2.SubmitNewPasswordCallback {
                    override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
    }

    @Test
    fun testAttributesRequiredStateCallbackReturnsInvalidState() {
        val attributes = UserAttributes.Builder().city("city").build()
        assertCallbackInvalidState { future ->
            AttributesRequiredStateV2(correlationId, scenario, config).submitAttributes(
                attributes,
                object : AttributesRequiredStateV2.SubmitAttributesCallback {
                    override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
    }

    @Test
    fun testAttributesInvalidStateCallbackReturnsInvalidState() {
        val attributes = UserAttributes.Builder().city("city").build()
        assertCallbackInvalidState { future ->
            AttributesInvalidStateV2(correlationId, scenario, config).submitAttributes(
                attributes,
                object : AttributesInvalidStateV2.SubmitAttributesCallback {
                    override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
    }

    @Test
    fun testMFARequiredStateCallbackReturnsInvalidState() {
        val authMethod = AuthMethod("id", "oob", null, "email")
        assertCallbackInvalidState { future ->
            MFARequiredStateV2(correlationId, scenario, config).selectAuthMethod(
                authMethod,
                callback = object : MFARequiredStateV2.SelectAuthMethodCallback {
                    override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
    }

    @Test
    fun testMFARequiredStateDefensivelyCopiesAuthMethods() {
        val source = mutableListOf(AuthMethod("id", "oob", null, "email"))
        val state = MFARequiredStateV2(
            correlationId,
            scenario,
            config,
            authMethods = source
        )

        source.clear()
        assertEquals(1, state.authMethods.size)
        try {
            (state.authMethods as MutableList<AuthMethod>).clear()
            fail("Expected authMethods to be unmodifiable")
        } catch (_: UnsupportedOperationException) {
            // Expected.
        }
    }

    @Test
    fun testMFAVerificationRequiredStateCallbackReturnsInvalidState() {
        assertCallbackInvalidState { future ->
            MFAVerificationRequiredStateV2(correlationId, scenario, config).submitChallenge(
                "challenge",
                object : MFAVerificationRequiredStateV2.SubmitChallengeCallback {
                    override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        assertCallbackInvalidState { future ->
            MFAVerificationRequiredStateV2(correlationId, scenario, config).resendChallenge(
                object : MFAVerificationRequiredStateV2.ResendChallengeCallback {
                    override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
    }

    @Test
    fun testStrongAuthRegistrationRequiredStateCallbackReturnsNotImplemented() {
        val authMethod = AuthMethod("id", "oob", null, "email")
        assertCallbackNotImplemented { future ->
            StrongAuthRegistrationRequiredStateV2(correlationId, scenario, config).selectAuthMethod(
                authMethod,
                callback = object : StrongAuthRegistrationRequiredStateV2.SelectAuthMethodCallback {
                    override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
    }

    @Test
    fun testStrongAuthVerificationRequiredStateCallbackReturnsNotImplemented() {
        assertCallbackNotImplemented { future ->
            StrongAuthVerificationRequiredStateV2(correlationId, scenario, config).submitChallenge(
                "challenge",
                object : StrongAuthVerificationRequiredStateV2.SubmitChallengeCallback {
                    override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
    }

    @Test
    fun testStateCallbacksRouteExceptionsToOnError() {
        assertCallbackRoutesToOnError { future, thrown ->
            CodeRequiredStateV2(correlationId, scenario, config).submitCode(
                "1234",
                object : CodeRequiredStateV2.SubmitCodeCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        assertCallbackRoutesToOnError { future, thrown ->
            CodeRequiredStateV2(correlationId, scenario, config).resendCode(
                object : CodeRequiredStateV2.ResendCodeCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        assertCallbackRoutesToOnError { future, thrown ->
            PasswordRequiredStateV2(correlationId, scenario, config).submitPassword(
                "password".toCharArray(),
                object : PasswordRequiredStateV2.SubmitPasswordCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        assertCallbackRoutesToOnError { future, thrown ->
            NewPasswordRequiredStateV2(correlationId, scenario, config).submitNewPassword(
                "password".toCharArray(),
                object : NewPasswordRequiredStateV2.SubmitNewPasswordCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        val attributes = UserAttributes.Builder().city("city").build()
        assertCallbackRoutesToOnError { future, thrown ->
            AttributesRequiredStateV2(correlationId, scenario, config).submitAttributes(
                attributes,
                object : AttributesRequiredStateV2.SubmitAttributesCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        assertCallbackRoutesToOnError { future, thrown ->
            AttributesInvalidStateV2(correlationId, scenario, config).submitAttributes(
                attributes,
                object : AttributesInvalidStateV2.SubmitAttributesCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        val authMethod = AuthMethod("id", "oob", null, "email")
        assertCallbackRoutesToOnError { future, thrown ->
            MFARequiredStateV2(correlationId, scenario, config).selectAuthMethod(
                authMethod,
                null,
                object : MFARequiredStateV2.SelectAuthMethodCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        assertCallbackRoutesToOnError { future, thrown ->
            ResetPasswordMethodRequiredStateV2(
                correlationId,
                NativeAuthFlowScenarioV2.RESET_PASSWORD,
                config
            ).selectAuthMethod(
                authMethod,
                object : ResetPasswordMethodRequiredStateV2.SelectAuthMethodCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        assertCallbackRoutesToOnError { future, thrown ->
            MFAVerificationRequiredStateV2(correlationId, scenario, config).submitChallenge(
                "challenge",
                object : MFAVerificationRequiredStateV2.SubmitChallengeCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        assertCallbackRoutesToOnError { future, thrown ->
            MFAVerificationRequiredStateV2(correlationId, scenario, config).resendChallenge(
                object : MFAVerificationRequiredStateV2.ResendChallengeCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        assertCallbackRoutesToOnError { future, thrown ->
            StrongAuthRegistrationRequiredStateV2(correlationId, scenario, config).selectAuthMethod(
                authMethod,
                null,
                object : StrongAuthRegistrationRequiredStateV2.SelectAuthMethodCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        assertCallbackRoutesToOnError { future, thrown ->
            StrongAuthVerificationRequiredStateV2(correlationId, scenario, config).submitChallenge(
                "challenge",
                object : StrongAuthVerificationRequiredStateV2.SubmitChallengeCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
    }
}
