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
import com.microsoft.identity.nativeauth.parameters.NativeAuthGetAccessTokenParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignUpParameters
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
        password: CharArray? = null
    ): NativeAuthResultV2.CodeRequired {
        val parameters = NativeAuthSignUpParameters(username = email)
        if (password != null) {
            parameters.password = password
        }

        val result = application.signUpV2(parameters)
        assertResult<NativeAuthResultV2.CodeRequired>(result)
        val codeRequired = result as NativeAuthResultV2.CodeRequired
        assertEquals("email", codeRequired.channel.lowercase())
        assertTrue(codeRequired.codeLength > 0)
        return codeRequired
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
        email: String
    ) {
        assertSignUpScenario(result)
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
