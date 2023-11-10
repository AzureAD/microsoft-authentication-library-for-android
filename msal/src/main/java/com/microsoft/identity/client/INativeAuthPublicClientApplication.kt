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
package com.microsoft.identity.client

import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.statemachine.results.ResetPasswordStartResult
import com.microsoft.identity.client.statemachine.results.SignInResult
import com.microsoft.identity.client.statemachine.results.SignInUsingPasswordResult
import com.microsoft.identity.client.statemachine.states.AccountResult

/**
 * INativeAuthPublicClientApplication provides top level interface that is used by app developers
 * to use Native Auth methods.
 */
interface INativeAuthPublicClientApplication : IPublicClientApplication {
    /**
     * Listener callback for asynchronous initialization of INativeAuthPublicClientApplication object.
     */
    interface INativeAuthApplicationCreatedListener {
        /**
         * Called once an INativeAuthPublicClientApplication is successfully created.
         */
        fun onCreated(application: INativeAuthPublicClientApplication)

        /**
         * Called once INativeAuthPublicClientApplication can't be created.
         */
        fun onError(exception: MsalException)
    }

    /**
     * Retrieve the current signed in account from cache; Kotlin coroutines variant.
     *
     * @return [com.microsoft.identity.client.statemachine.states.AccountResult] if there is a signed in account, null otherwise.
     */
    suspend fun getCurrentAccount(): AccountResult?

    /**
     * Retrieve the current signed in account from cache; Kotlin coroutines variant.
     *
     * @return [com.microsoft.identity.client.statemachine.states.AccountResult] if there is a signed in account, null otherwise.
     */
    fun getCurrentAccount(callback: NativeAuthPublicClientApplication.GetCurrentAccountCallback)

    /**
     * Sign in a user with a given username; Kotlin coroutines variant.
     *
     * @param username username of the account to sign in.
     * @param scopes (Optional) scopes to request during the sign in.
     * @return [com.microsoft.identity.client.statemachine.results.SignInResult] see detailed possible return state under the object.
     * @throws [MsalException] if an account is already signed in.
     */
    suspend fun signIn(username: String, scopes: List<String>? = null): SignInResult

    /**
     * Sign in a user with a given username; callback variant.
     *
     * @param username username of the account to sign in.
     * @param scopes (Optional) scopes to request during the sign in.
     * @param callback [com.microsoft.identity.client.NativeAuthPublicClientApplication.SignInCallback] to receive the result.
     * @return [com.microsoft.identity.client.statemachine.results.SignInResult] see detailed possible return state under the object.
     * @throws [MsalException] if an account is already signed in.
     */
    fun signIn(username: String, scopes: List<String>? = null, callback: NativeAuthPublicClientApplication.SignInCallback)

    /**
     * Sign in the account using username and password; Kotlin coroutines variant.
     *
     * @param username username of the account to sign in.
     * @param password password of the account to sign in.
     * @param scopes (Optional) list of scopes to request.
     * @return [com.microsoft.identity.client.statemachine.results.SignInUsingPasswordResult] see detailed possible return state under the object.
     * @throws MsalClientException if an account is already signed in.
     */
    suspend fun signInUsingPassword(username: String, password: CharArray, scopes: List<String>? = null): SignInUsingPasswordResult

    /**
     * Sign in the account using username and password; callback variant.
     *
     * @param username username of the account to sign in.
     * @param password password of the account to sign in.
     * @param scopes (Optional) list of scopes to request.
     * @param callback [com.microsoft.identity.client.NativeAuthPublicClientApplication.SignInUsingPasswordCallback] to receive the result.
     * @return [com.microsoft.identity.client.statemachine.results.SignInUsingPasswordResult] see detailed possible return state under the object.
     * @throws MsalClientException if an account is already signed in.
     */
    fun signInUsingPassword(username: String, password: CharArray, scopes: List<String>? = null, callback: NativeAuthPublicClientApplication.SignInUsingPasswordCallback)

    suspend fun resetPassword(username: String): ResetPasswordStartResult

    fun resetPassword(username: String, callback: NativeAuthPublicClientApplication.ResetPasswordCallback)
}
