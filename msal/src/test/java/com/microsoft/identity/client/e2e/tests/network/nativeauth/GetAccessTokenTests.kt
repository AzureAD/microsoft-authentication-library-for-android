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

package com.microsoft.identity.client.e2e.tests.network.nativeauth

import com.microsoft.identity.client.e2e.utils.assertState
import com.microsoft.identity.internal.testutils.nativeauth.NativeAuthCredentialHelper
import com.microsoft.identity.nativeauth.statemachine.errors.SignInError
import com.microsoft.identity.nativeauth.statemachine.results.GetAccessTokenResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class GetAccessTokenTests : NativeAuthPublicClientApplicationAbstractTest() {

    @Test
    fun test1() = runTest {
        val username = NativeAuthCredentialHelper.nativeAuthSignInUsername
        val password = getSafePassword()
        val scopes = listOf("")
        val result = application.signIn(
            username = username,
            password = password.toCharArray(),
            scopes = scopes
        )
        assertState(result, SignInResult.Complete::class.java)
        val accountState = (result as SignInResult.Complete).resultValue
        val initialAccessToken = accountState.authenticationResult!!.accessToken

        // Token should be retrieved from cache, and so should be the same as the previously returned token
        val getAccessTokenResult = accountState.getAccessToken()
        assertState(getAccessTokenResult, GetAccessTokenResult.Complete::class.java)
        val retrievedAccessToken = (getAccessTokenResult as GetAccessTokenResult.Complete).resultValue.accessToken
        Assert.assertEquals(retrievedAccessToken, initialAccessToken)

        // Token should be refreshed, and so should not be the same as the previously returned token
        val getAccessTokenResult2 = accountState.getAccessToken(forceRefresh = true)
        Assert.assertTrue(getAccessTokenResult2 is GetAccessTokenResult.Complete)
        val refreshedAccessToken = (getAccessTokenResult2 as GetAccessTokenResult.Complete).resultValue.accessToken
        Assert.assertNotEquals(refreshedAccessToken, initialAccessToken)
    }
}