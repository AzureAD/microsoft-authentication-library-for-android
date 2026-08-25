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
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import com.microsoft.identity.common.java.util.ResultFuture
import com.microsoft.identity.nativeauth.AuthMethod
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.UserAttributes
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.statemachine.states.AttributesInvalidStateV2
import com.microsoft.identity.nativeauth.statemachine.states.AttributesRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.CodeRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.MFARequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.MFAVerificationRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.NativeAuthBaseStateV2
import com.microsoft.identity.nativeauth.statemachine.states.NewPasswordRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.PasswordRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.SignInAfterResetPasswordStateV2
import com.microsoft.identity.nativeauth.statemachine.states.StrongAuthRegistrationRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.StrongAuthVerificationRequiredStateV2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertNull
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

    private val continuationToken = "continuation-token"
    private val correlationId = "correlation-id"
    private val scenario = NativeAuthFlowScenarioV2.SIGN_IN
    private val config = NativeAuthPublicClientApplicationConfiguration()

    private fun <T : NativeAuthBaseStateV2> assertParcelRoundTrip(
        state: T,
        creator: Parcelable.Creator<T>
    ): T {
        assertEquals(0, state.describeContents())
        val parcel = Parcel.obtain()
        try {
            state.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            val restored = creator.createFromParcel(parcel)
            assertEquals(state.continuationToken, restored.continuationToken)
            assertEquals(state.correlationId, restored.correlationId)
            assertEquals(state.scenario, restored.scenario)
            assertEquals(1, creator.newArray(1).size)
            return restored
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun testStatesAreParcelable() {
        assertParcelRoundTrip(
            CodeRequiredStateV2(continuationToken, correlationId, scenario, config),
            CodeRequiredStateV2.CREATOR
        )
        assertParcelRoundTrip(
            PasswordRequiredStateV2(continuationToken, correlationId, scenario, config),
            PasswordRequiredStateV2.CREATOR
        )
        assertParcelRoundTrip(
            NewPasswordRequiredStateV2(continuationToken, correlationId, scenario, config),
            NewPasswordRequiredStateV2.CREATOR
        )
        assertParcelRoundTrip(
            AttributesRequiredStateV2(continuationToken, correlationId, scenario, config),
            AttributesRequiredStateV2.CREATOR
        )
        assertParcelRoundTrip(
            AttributesInvalidStateV2(continuationToken, correlationId, scenario, config),
            AttributesInvalidStateV2.CREATOR
        )
        assertParcelRoundTrip(
            MFARequiredStateV2(continuationToken, correlationId, scenario, config),
            MFARequiredStateV2.CREATOR
        )
        assertParcelRoundTrip(
            MFAVerificationRequiredStateV2(continuationToken, correlationId, scenario, config),
            MFAVerificationRequiredStateV2.CREATOR
        )
        assertParcelRoundTrip(
            StrongAuthRegistrationRequiredStateV2(continuationToken, correlationId, scenario, config),
            StrongAuthRegistrationRequiredStateV2.CREATOR
        )
        assertParcelRoundTrip(
            StrongAuthVerificationRequiredStateV2(continuationToken, correlationId, scenario, config),
            StrongAuthVerificationRequiredStateV2.CREATOR
        )
    }

    @Test
    fun testContinuationStatesParcelOpaqueStateWithoutInventingContinuationToken() {
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

        listOf(restoredCodeState, restoredPasswordState, restoredSignInState).forEach { restored ->
            assertNull(restored.continuationToken)
            assertEquals(correlationId, restored.correlationId)
            assertEquals(listOf("scope"), restored.continuationState?.scopesForTokenRequest())
        }
    }

    private fun createContinuationState(): NativeAuthV2ContinuationState {
        val constructor = NativeAuthV2ContinuationState::class.java.declaredConstructors
            .single { it.parameterCount == 7 }
        constructor.isAccessible = true
        return constructor.newInstance(
            "opaque-token",
            emptyMap<String, String>(),
            listOf("scope"),
            null,
            correlationId,
            NativeAuthV2LinkRelation.RESET_PASSWORD.value,
            NativeAuthV2FlowScenario.RESET_PASSWORD
        ) as NativeAuthV2ContinuationState
    }

    private fun assertCallbackNotImplemented(action: (ResultFuture<NativeAuthResultV2>) -> Unit) {
        val future = ResultFuture<NativeAuthResultV2>()
        action(future)
        val result = future.get(30, TimeUnit.SECONDS)
        assertTrue(result is NativeAuthErrorV2)
        assertTrue((result as NativeAuthErrorV2).isNotImplemented())
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
    fun testCodeRequiredStateCallbacksReturnNotImplemented() {
        assertCallbackNotImplemented { future ->
            CodeRequiredStateV2(continuationToken, correlationId, scenario, config).submitCode(
                "1234",
                object : CodeRequiredStateV2.SubmitCodeCallback {
                    override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        assertCallbackNotImplemented { future ->
            CodeRequiredStateV2(continuationToken, correlationId, scenario, config).resendCode(
                object : CodeRequiredStateV2.ResendCodeCallback {
                    override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
    }

    @Test
    fun testPasswordRequiredStateCallbackReturnsNotImplemented() {
        assertCallbackNotImplemented { future ->
            PasswordRequiredStateV2(continuationToken, correlationId, scenario, config).submitPassword(
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
            NewPasswordRequiredStateV2(continuationToken, correlationId, scenario, config).submitNewPassword(
                "password".toCharArray(),
                object : NewPasswordRequiredStateV2.SubmitNewPasswordCallback {
                    override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
    }

    @Test
    fun testAttributesRequiredStateCallbackReturnsNotImplemented() {
        val attributes = UserAttributes.Builder().city("city").build()
        assertCallbackNotImplemented { future ->
            AttributesRequiredStateV2(continuationToken, correlationId, scenario, config).submitAttributes(
                attributes,
                object : AttributesRequiredStateV2.SubmitAttributesCallback {
                    override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
    }

    @Test
    fun testAttributesInvalidStateCallbackReturnsNotImplemented() {
        val attributes = UserAttributes.Builder().city("city").build()
        assertCallbackNotImplemented { future ->
            AttributesInvalidStateV2(continuationToken, correlationId, scenario, config).submitAttributes(
                attributes,
                object : AttributesInvalidStateV2.SubmitAttributesCallback {
                    override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
    }

    @Test
    fun testMFARequiredStateCallbackReturnsNotImplemented() {
        val authMethod = AuthMethod("id", "oob", null, "email")
        assertCallbackNotImplemented { future ->
            MFARequiredStateV2(continuationToken, correlationId, scenario, config).selectAuthMethod(
                authMethod,
                callback = object : MFARequiredStateV2.SelectAuthMethodCallback {
                    override fun onResult(result: NativeAuthResultV2) = future.setResult(result)
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
    }

    @Test
    fun testMFAVerificationRequiredStateCallbackReturnsNotImplemented() {
        assertCallbackNotImplemented { future ->
            MFAVerificationRequiredStateV2(continuationToken, correlationId, scenario, config).submitChallenge(
                "challenge",
                object : MFAVerificationRequiredStateV2.SubmitChallengeCallback {
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
            StrongAuthRegistrationRequiredStateV2(continuationToken, correlationId, scenario, config).selectAuthMethod(
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
            StrongAuthVerificationRequiredStateV2(continuationToken, correlationId, scenario, config).submitChallenge(
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
            CodeRequiredStateV2(continuationToken, correlationId, scenario, config).submitCode(
                "1234",
                object : CodeRequiredStateV2.SubmitCodeCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        assertCallbackRoutesToOnError { future, thrown ->
            CodeRequiredStateV2(continuationToken, correlationId, scenario, config).resendCode(
                object : CodeRequiredStateV2.ResendCodeCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        assertCallbackRoutesToOnError { future, thrown ->
            PasswordRequiredStateV2(continuationToken, correlationId, scenario, config).submitPassword(
                "password".toCharArray(),
                object : PasswordRequiredStateV2.SubmitPasswordCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        assertCallbackRoutesToOnError { future, thrown ->
            NewPasswordRequiredStateV2(continuationToken, correlationId, scenario, config).submitNewPassword(
                "password".toCharArray(),
                object : NewPasswordRequiredStateV2.SubmitNewPasswordCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        val attributes = UserAttributes.Builder().city("city").build()
        assertCallbackRoutesToOnError { future, thrown ->
            AttributesRequiredStateV2(continuationToken, correlationId, scenario, config).submitAttributes(
                attributes,
                object : AttributesRequiredStateV2.SubmitAttributesCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        assertCallbackRoutesToOnError { future, thrown ->
            AttributesInvalidStateV2(continuationToken, correlationId, scenario, config).submitAttributes(
                attributes,
                object : AttributesInvalidStateV2.SubmitAttributesCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        val authMethod = AuthMethod("id", "oob", null, "email")
        assertCallbackRoutesToOnError { future, thrown ->
            MFARequiredStateV2(continuationToken, correlationId, scenario, config).selectAuthMethod(
                authMethod,
                null,
                object : MFARequiredStateV2.SelectAuthMethodCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        assertCallbackRoutesToOnError { future, thrown ->
            MFAVerificationRequiredStateV2(continuationToken, correlationId, scenario, config).submitChallenge(
                "challenge",
                object : MFAVerificationRequiredStateV2.SubmitChallengeCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        assertCallbackRoutesToOnError { future, thrown ->
            StrongAuthRegistrationRequiredStateV2(continuationToken, correlationId, scenario, config).selectAuthMethod(
                authMethod,
                null,
                object : StrongAuthRegistrationRequiredStateV2.SelectAuthMethodCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
        assertCallbackRoutesToOnError { future, thrown ->
            StrongAuthVerificationRequiredStateV2(continuationToken, correlationId, scenario, config).submitChallenge(
                "challenge",
                object : StrongAuthVerificationRequiredStateV2.SubmitChallengeCallback {
                    override fun onResult(result: NativeAuthResultV2): Unit = throw thrown
                    override fun onError(exception: BaseException) = future.setException(exception)
                }
            )
        }
    }
}
