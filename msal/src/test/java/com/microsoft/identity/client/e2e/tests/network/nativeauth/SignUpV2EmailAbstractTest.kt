// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.e2e.tests.network.nativeauth

import com.microsoft.identity.client.e2e.utils.NativeAuthRequestRecorder
import com.microsoft.identity.client.e2e.utils.assertResult as assertResultType
import com.microsoft.identity.internal.testutils.nativeauth.ConfigType
import com.microsoft.identity.internal.testutils.nativeauth.api.TemporaryEmailService
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.UserAttributes
import com.microsoft.identity.nativeauth.parameters.NativeAuthGetAccessTokenParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignUpParametersV2
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.results.GetAccessTokenResult
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import java.util.UUID

abstract class SignUpV2EmailAbstractTest : NativeAuthPublicClientApplicationAbstractTest() {
    protected val tempEmailApi = TemporaryEmailService()

    protected companion object {
        const val VALID_CITY = "Dublin"
        const val VALID_COUNTRY = "Ireland"
    }

    protected fun cityCountryAttributes(): UserAttributes =
        UserAttributes.Builder()
            .city(VALID_CITY)
            .country(VALID_COUNTRY)
            .build()

    protected fun assertCityCountryAttributesRequired(
        result: NativeAuthResultV2.AttributesRequired
    ) {
        assertEquals(2, result.requiredAttributes.size)
        assertEquals(
            setOf("city", "country"),
            result.requiredAttributes.map { it.attributeName }.toSet()
        )
        assertTrue(result.requiredAttributes.all { it.required == true })
    }

    protected inline fun <reified ExpectedType> assertResult(actual: Any) {
        assertResultType<ExpectedType>(actual)
        if (actual is NativeAuthResultV2) {
            assertSignUpScenario(actual)
        }
    }

    protected fun assertSignUpScenario(result: NativeAuthResultV2) {
        assertEquals(NativeAuthFlowScenarioV2.SIGN_UP, result.scenario)
    }

    protected fun createApplication(
        configType: ConfigType,
        expectedClientId: String,
        recorder: NativeAuthRequestRecorder? = null
    ): INativeAuthPublicClientApplication {
        val config = getConfig(configType)
        assertEquals(expectedClientId, config.clientId)
        return setupPCA(
            config,
            listOf("password", "oob"),
            listOf("mfa_required", "registration_required"),
            recorder
        )
    }

    protected suspend fun startSignUp(
        application: INativeAuthPublicClientApplication,
        email: String,
        attributes: UserAttributes? = null
    ): NativeAuthResultV2.CodeRequired {
        val parameters = NativeAuthSignUpParametersV2(username = email)
        parameters.attributes = attributes

        val result = application.signUpV2(parameters)
        assertResult<NativeAuthResultV2.CodeRequired>(result)
        val codeRequired = result as NativeAuthResultV2.CodeRequired
        assertEquals("email", codeRequired.channel.lowercase())
        assertTrue(codeRequired.codeLength > 0)
        return codeRequired
    }

    protected suspend fun submitSignUpPassword(
        result: NativeAuthResultV2,
        password: CharArray
    ): NativeAuthResultV2 {
        assertResult<NativeAuthResultV2.PasswordRequired>(result)
        return (result as NativeAuthResultV2.PasswordRequired).nextState.submitPassword(password)
    }

    protected suspend fun signInExistingAccount(
        result: NativeAuthResultV2,
        email: String,
        password: CharArray? = null
    ): NativeAuthResultV2.Complete {
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.scenario)
        val challenge = if (result is NativeAuthResultV2.AuthMethodSelectionRequired) {
            val channel = if (password == null) "email" else "password"
            val method = result.authMethods.single { it.challengeChannel == channel }
            result.nextState.selectAuthMethod(method)
        } else {
            result
        }
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, challenge.scenario)
        val signedIn = when (challenge) {
            is NativeAuthResultV2.ExistingAccountCodeRequired ->
                challenge.nextState.submitCode(tempEmailApi.retrieveCodeFromInbox(email))
            is NativeAuthResultV2.ExistingAccountPasswordRequired -> {
                assertNotNull(password)
                challenge.nextState.submitPassword(requireNotNull(password))
            }
            else -> throw AssertionError("Expected an existing-account sign-in challenge")
        }
        assertResultType<NativeAuthResultV2.Complete>(signedIn)
        val complete = signedIn as NativeAuthResultV2.Complete
        assertAccountAndTokens(complete, email, NativeAuthFlowScenarioV2.SIGN_IN)
        return complete
    }

    protected suspend fun completeSignInAfterSignUp(
        result: NativeAuthResultV2.SignInAfterSignUpRequired
    ): NativeAuthResultV2.Complete {
        assertSignUpScenario(result)
        val complete = result.nextState.signIn()
        assertResult<NativeAuthResultV2.Complete>(complete)
        return complete as NativeAuthResultV2.Complete
    }

    protected suspend fun assertAccountAndTokens(
        result: NativeAuthResultV2.Complete,
        email: String,
        scenario: NativeAuthFlowScenarioV2 = NativeAuthFlowScenarioV2.SIGN_UP
    ) {
        assertEquals(scenario, result.scenario)
        val accountState = result.resultValue
        val account = accountState.getAccount()
        assertNotNull(account)
        assertTrue(account.username.equals(email, ignoreCase = true))
        assertFalse(accountState.getIdToken().isNullOrBlank())

        val tokenResult = accountState.getAccessToken(NativeAuthGetAccessTokenParameters())
        assertResult<GetAccessTokenResult.Complete>(tokenResult)
        val authenticationResult = (tokenResult as GetAccessTokenResult.Complete).resultValue
        assertFalse(authenticationResult.accessToken.isNullOrBlank())
        assertNotNull(authenticationResult.account)
    }

    protected fun newValidPassword(): CharArray =
        "Aa1!${UUID.randomUUID().toString().replace("-", "")}".toCharArray()
}
