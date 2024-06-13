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

import com.microsoft.identity.client.e2e.shadows.ShadowBaseController
import com.microsoft.identity.client.e2e.utils.assertState
import com.microsoft.identity.internal.testutils.nativeauth.NativeAuthCredentialHelper
import com.microsoft.identity.nativeauth.statemachine.errors.GetAccessTokenError
import com.microsoft.identity.nativeauth.statemachine.results.GetAccessTokenResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.robolectric.annotation.Config

@Config(shadows = [ShadowBaseController::class])
class GetAccessTokenTests : NativeAuthPublicClientApplicationAbstractTest() {
    companion object {
        const val EMPLOYEE_WRITE_ALL_SCOPE = "api://1e9e882d-3f7d-4b02-af06-b5db4d8466c0/Employees.Write.All"
        const val EMPLOYEE_READ_ALL_SCOPE = "api://1e9e882d-3f7d-4b02-af06-b5db4d8466c0/Employees.Read.All"
        const val CUSTOMERS_WRITE_ALL_SCOPE = "api://a6568f2f-47a5-4b18-b2c7-25eff03d87d6/Customers.Write.All"
        const val CUSTOMERS_READ_ALL_SCOPE = "api://a6568f2f-47a5-4b18-b2c7-25eff03d87d6/Customers.Read.All"
    }

    @Test
    fun testGetAccessTokenCompareForceRefreshBehaviour() = runTest {
        val username = NativeAuthCredentialHelper.nativeAuthSignInUsername
        val password = getSafePassword()
        val result = application.signIn(
            username = username,
            password = password.toCharArray(),
            scopes = listOf(EMPLOYEE_WRITE_ALL_SCOPE)
        )
        assertState(result, SignInResult.Complete::class.java)
        val accountState = (result as SignInResult.Complete).resultValue

        // Var to keep track of whether BaseController.renewAccessToken() was called. This method calls the API to refresh the access token, for example if it's expired or not available in cache.
        var wasRenewAccessTokenInvoked = false
        ShadowBaseController.setOnRenewAccessTokenInvokedCallback { wasRenewAccessTokenInvoked = true }

        // Token should be retrieved from cache
        val getAccessTokenResult = accountState.getAccessToken()
        assertState(getAccessTokenResult, GetAccessTokenResult.Complete::class.java)
        Assert.assertFalse(wasRenewAccessTokenInvoked)
        val retrievedAccessToken = (getAccessTokenResult as GetAccessTokenResult.Complete).resultValue.accessToken

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should be refreshed, and so should not be the same as the previously returned token
        val getAccessTokenResult2 = accountState.getAccessToken(forceRefresh = true)
        Assert.assertTrue(getAccessTokenResult2 is GetAccessTokenResult.Complete)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val refreshedAccessToken = (getAccessTokenResult2 as GetAccessTokenResult.Complete).resultValue.accessToken
        Assert.assertNotEquals(refreshedAccessToken, retrievedAccessToken)
    }

    @Test
    fun testGetAccessTokenFromCache() = runTest {
        val username = NativeAuthCredentialHelper.nativeAuthSignInUsername
        val password = getSafePassword()
        val result = application.signIn(
            username = username,
            password = password.toCharArray(),
            scopes = listOf(EMPLOYEE_WRITE_ALL_SCOPE)
        )
        assertState(result, SignInResult.Complete::class.java)
        val accountState = (result as SignInResult.Complete).resultValue

        // Var to keep track of whether BaseController.renewAccessToken() was called. This method calls the API to refresh the access token, for example if it's expired or not available in cache.
        var wasRenewAccessTokenInvoked = false
        ShadowBaseController.setOnRenewAccessTokenInvokedCallback { wasRenewAccessTokenInvoked = true }

        // Token should be retrieved from cache
        val getAccessTokenResult = accountState.getAccessToken()
        assertState(getAccessTokenResult, GetAccessTokenResult.Complete::class.java)
        Assert.assertFalse(wasRenewAccessTokenInvoked)
        val retrievedAccessToken = (getAccessTokenResult as GetAccessTokenResult.Complete).resultValue.accessToken

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should not be refreshed, and so should be the same as the previously returned token
        val getAccessTokenResult2 = accountState.getAccessToken(scopes = listOf(EMPLOYEE_WRITE_ALL_SCOPE))
        Assert.assertTrue(getAccessTokenResult2 is GetAccessTokenResult.Complete)
        Assert.assertFalse(wasRenewAccessTokenInvoked)
        val authResult = (getAccessTokenResult2 as GetAccessTokenResult.Complete).resultValue
        val accessTokenForExplicitScopes = authResult.accessToken
        Assert.assertEquals(accessTokenForExplicitScopes, retrievedAccessToken)
        Assert.assertTrue(authResult.scope.contains(EMPLOYEE_WRITE_ALL_SCOPE))
    }

    @Test
    fun testGetAccessTokenWith1CustomApiResource() = runTest {
        val username = NativeAuthCredentialHelper.nativeAuthSignInUsername
        val password = getSafePassword()
        val result = application.signIn(
            username = username,
            password = password.toCharArray(),
            scopes = listOf(EMPLOYEE_WRITE_ALL_SCOPE)
        )
        assertState(result, SignInResult.Complete::class.java)
        val accountState = (result as SignInResult.Complete).resultValue

        // Var to keep track of whether BaseController.renewAccessToken() was called. This method calls the API to refresh the access token, for example if it's expired or not available in cache.
        var wasRenewAccessTokenInvoked = false
        ShadowBaseController.setOnRenewAccessTokenInvokedCallback { wasRenewAccessTokenInvoked = true }

        // Token should be retrieved from cache
        val getAccessTokenResult1 = accountState.getAccessToken()
        assertState(getAccessTokenResult1, GetAccessTokenResult.Complete::class.java)
        Assert.assertFalse(wasRenewAccessTokenInvoked)
        val authResult1 = (getAccessTokenResult1 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult1.scope.contains(EMPLOYEE_WRITE_ALL_SCOPE))
        val tokenWithEmployeeScope = authResult1.accessToken

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should be retrieved from API, as the scope belongs to a different API resource
        val getAccessTokenResult2 = accountState.getAccessToken(
            forceRefresh = false,
            scopes = listOf(CUSTOMERS_READ_ALL_SCOPE)
        )
        assertState(getAccessTokenResult2, GetAccessTokenResult.Complete::class.java)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val authResult2 = (getAccessTokenResult2 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult2.scope.contains(CUSTOMERS_READ_ALL_SCOPE))
        val tokenWithCustomerScope = authResult2.accessToken
        Assert.assertNotEquals(tokenWithEmployeeScope, tokenWithCustomerScope)
    }

    @Test
    fun testGetAccessTokenWith2CustomApiResources() = runTest {
        val username = NativeAuthCredentialHelper.nativeAuthSignInUsername
        val password = getSafePassword()
        val result = application.signIn(
            username = username,
            password = password.toCharArray()
        )
        assertState(result, SignInResult.Complete::class.java)
        val accountState = (result as SignInResult.Complete).resultValue

        // Var to keep track of whether BaseController.renewAccessToken() was called. This method calls the API to refresh the access token, for example if it's expired or not available in cache.
        var wasRenewAccessTokenInvoked = false
        ShadowBaseController.setOnRenewAccessTokenInvokedCallback { wasRenewAccessTokenInvoked = true }

        // Token should be retrieved from API
        val getAccessTokenResult1 = accountState.getAccessToken(scopes = listOf(EMPLOYEE_WRITE_ALL_SCOPE))
        assertState(getAccessTokenResult1, GetAccessTokenResult.Complete::class.java)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val authResult1 = (getAccessTokenResult1 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult1.scope.contains(EMPLOYEE_WRITE_ALL_SCOPE))
        val tokenWithEmployeeScope = authResult1.accessToken

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should be retrieved from API, as the scope belongs to a different API resource
        val getAccessTokenResult2 = accountState.getAccessToken(forceRefresh = false, scopes = listOf(CUSTOMERS_READ_ALL_SCOPE))
        assertState(getAccessTokenResult2, GetAccessTokenResult.Complete::class.java)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val authResult2 = (getAccessTokenResult2 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult2.scope.contains(CUSTOMERS_READ_ALL_SCOPE))
        val tokenWithCustomerScope = authResult2.accessToken
        Assert.assertNotEquals(tokenWithEmployeeScope, tokenWithCustomerScope)
    }

    @Test
    fun testSuperSetOfScopesFor1APIResource() = runTest {
        val username = NativeAuthCredentialHelper.nativeAuthSignInUsername
        val password = getSafePassword()
        val result = application.signIn(
            username = username,
            password = password.toCharArray(),
            scopes = listOf(EMPLOYEE_READ_ALL_SCOPE)
        )

        assertState(result, SignInResult.Complete::class.java)
        val accountState = (result as SignInResult.Complete).resultValue

        // Var to keep track of whether BaseController.renewAccessToken() was called. This method calls the API to refresh the access token, for example if it's expired or not available in cache.
        var wasRenewAccessTokenInvoked = false
        ShadowBaseController.setOnRenewAccessTokenInvokedCallback { wasRenewAccessTokenInvoked = true }

        // Token should be retrieved from cache
        val getAccessTokenResult1 = accountState.getAccessToken(scopes = listOf(EMPLOYEE_WRITE_ALL_SCOPE))
        assertState(getAccessTokenResult1, GetAccessTokenResult.Complete::class.java)
        Assert.assertFalse(wasRenewAccessTokenInvoked)
        val authResult1 = (getAccessTokenResult1 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult1.scope.contains(EMPLOYEE_WRITE_ALL_SCOPE))
        Assert.assertTrue(authResult1.scope.contains(EMPLOYEE_READ_ALL_SCOPE))
        val tokenWithEmployeeScope = authResult1.accessToken

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should be retrieved from API, as the scope belongs to a different API resource
        val getAccessTokenResult2 = accountState.getAccessToken(forceRefresh = false, scopes = listOf(CUSTOMERS_READ_ALL_SCOPE))
        assertState(getAccessTokenResult2, GetAccessTokenResult.Complete::class.java)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val authResult2 = (getAccessTokenResult2 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult2.scope.contains(CUSTOMERS_READ_ALL_SCOPE))
        Assert.assertTrue(authResult2.scope.contains(CUSTOMERS_WRITE_ALL_SCOPE))
        val tokenWithCustomerScope = authResult2.accessToken
        Assert.assertNotEquals(tokenWithEmployeeScope, tokenWithCustomerScope)
    }

    @Test
    fun testGetAccessTokenWithMultipleAPIResourceScopesShouldReturnError() = runTest {
        val username = NativeAuthCredentialHelper.nativeAuthSignInUsername
        val password = getSafePassword()
        val result = application.signIn(
            username = username,
            password = password.toCharArray()
        )

        assertState(result, SignInResult.Complete::class.java)
        val accountState = (result as SignInResult.Complete).resultValue

        // Var to keep track of whether BaseController.renewAccessToken() was called. This method calls the API to refresh the access token, for example if it's expired or not available in cache.
        var wasRenewAccessTokenInvoked = false
        ShadowBaseController.setOnRenewAccessTokenInvokedCallback { wasRenewAccessTokenInvoked = true }

        // Token should be retrieved from cache
        val getAccessTokenResult1 = accountState.getAccessToken(scopes = listOf(EMPLOYEE_WRITE_ALL_SCOPE, CUSTOMERS_WRITE_ALL_SCOPE))
        assertState(getAccessTokenResult1, GetAccessTokenError::class.java)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
    }

    @Test
    fun testGetAccessTokenWith2CustomApiResourcesComplexCacheVerification() = runTest {
        val username = NativeAuthCredentialHelper.nativeAuthSignInUsername
        val password = getSafePassword()
        val result = application.signIn(
            username = username,
            password = password.toCharArray()
        )
        assertState(result, SignInResult.Complete::class.java)
        val accountState = (result as SignInResult.Complete).resultValue

        // Var to keep track of whether BaseController.renewAccessToken() was called. This method calls the API to refresh the access token, for example if it's expired or not available in cache.
        var wasRenewAccessTokenInvoked = false
        ShadowBaseController.setOnRenewAccessTokenInvokedCallback { wasRenewAccessTokenInvoked = true }

        // Token should be retrieved from API
        val getAccessTokenResult1 = accountState.getAccessToken(scopes = listOf(EMPLOYEE_WRITE_ALL_SCOPE))
        assertState(getAccessTokenResult1, GetAccessTokenResult.Complete::class.java)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val authResult1 = (getAccessTokenResult1 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult1.scope.contains(EMPLOYEE_WRITE_ALL_SCOPE))
        val tokenWithEmployeeScope = authResult1.accessToken

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should be retrieved from cache this time
        val getAccessTokenResult2 = accountState.getAccessToken(scopes = listOf(EMPLOYEE_WRITE_ALL_SCOPE))
        assertState(getAccessTokenResult2, GetAccessTokenResult.Complete::class.java)
        Assert.assertFalse(wasRenewAccessTokenInvoked)
        val authResult2 = (getAccessTokenResult2 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult2.scope.contains(EMPLOYEE_WRITE_ALL_SCOPE))
        val tokenWithEmployeeScope2 = authResult2.accessToken
        Assert.assertEquals(tokenWithEmployeeScope, tokenWithEmployeeScope2)

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should be retrieved from API, as the scope belongs to a different API resource
        val getAccessTokenResult3 = accountState.getAccessToken(forceRefresh = false, scopes = listOf(CUSTOMERS_READ_ALL_SCOPE))
        assertState(getAccessTokenResult3, GetAccessTokenResult.Complete::class.java)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val authResult3 = (getAccessTokenResult3 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult3.scope.contains(CUSTOMERS_READ_ALL_SCOPE))
        val tokenWithCustomerScope = authResult3.accessToken
        Assert.assertNotEquals(tokenWithEmployeeScope, tokenWithCustomerScope)

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should be retrieved from cache this time
        val getAccessTokenResult4 = accountState.getAccessToken(forceRefresh = false, scopes = listOf(CUSTOMERS_READ_ALL_SCOPE))
        assertState(getAccessTokenResult4, GetAccessTokenResult.Complete::class.java)
        Assert.assertFalse(wasRenewAccessTokenInvoked)
        val authResult4 = (getAccessTokenResult4 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult4.scope.contains(CUSTOMERS_READ_ALL_SCOPE))
        val tokenWithCustomerScope2 = authResult4.accessToken
        Assert.assertEquals(tokenWithCustomerScope, tokenWithCustomerScope2)
    }

    @Test
    fun testGetAccessTokenWithForceRefresh() = runTest {
        val username = NativeAuthCredentialHelper.nativeAuthSignInUsername
        val password = getSafePassword()
        val result = application.signIn(
            username = username,
            password = password.toCharArray()
        )
        assertState(result, SignInResult.Complete::class.java)
        val accountState = (result as SignInResult.Complete).resultValue

        // Var to keep track of whether BaseController.renewAccessToken() was called. This method calls the API to refresh the access token, for example if it's expired or not available in cache.
        var wasRenewAccessTokenInvoked = false
        ShadowBaseController.setOnRenewAccessTokenInvokedCallback { wasRenewAccessTokenInvoked = true }

        // Token should be retrieved from API
        val getAccessTokenResult1 = accountState.getAccessToken(scopes = listOf(EMPLOYEE_WRITE_ALL_SCOPE))
        assertState(getAccessTokenResult1, GetAccessTokenResult.Complete::class.java)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val authResult1 = (getAccessTokenResult1 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult1.scope.contains(EMPLOYEE_WRITE_ALL_SCOPE))
        val tokenWithEmployeeScope = authResult1.accessToken

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should be retrieved from API, due to force_refresh
        val getAccessTokenResult2 = accountState.getAccessToken(scopes = listOf(EMPLOYEE_WRITE_ALL_SCOPE), forceRefresh = true)
        assertState(getAccessTokenResult2, GetAccessTokenResult.Complete::class.java)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val authResult2 = (getAccessTokenResult2 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult2.scope.contains(EMPLOYEE_WRITE_ALL_SCOPE))
        val tokenWithEmployeeScope2 = authResult2.accessToken
        Assert.assertNotEquals(tokenWithEmployeeScope, tokenWithEmployeeScope2) // New token received

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should be retrieved from API, as the scope belongs to a different API resource
        val getAccessTokenResult3 = accountState.getAccessToken(forceRefresh = false, scopes = listOf(CUSTOMERS_READ_ALL_SCOPE))
        assertState(getAccessTokenResult3, GetAccessTokenResult.Complete::class.java)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val authResult3 = (getAccessTokenResult3 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult3.scope.contains(CUSTOMERS_READ_ALL_SCOPE))
        val tokenWithCustomerScope = authResult3.accessToken
        Assert.assertNotEquals(tokenWithEmployeeScope, tokenWithCustomerScope)

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should be retrieved from API, due to force_refresh
        val getAccessTokenResult4 = accountState.getAccessToken(forceRefresh = true, scopes = listOf(CUSTOMERS_READ_ALL_SCOPE))
        assertState(getAccessTokenResult4, GetAccessTokenResult.Complete::class.java)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val authResult4 = (getAccessTokenResult4 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult4.scope.contains(CUSTOMERS_READ_ALL_SCOPE))
        val tokenWithCustomerScope2 = authResult4.accessToken
        Assert.assertNotEquals(tokenWithCustomerScope, tokenWithCustomerScope2) // New token received
    }
}