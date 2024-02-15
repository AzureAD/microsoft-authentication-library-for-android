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
package com.microsoft.identity.nativeauth

import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.nativeauth.statemachine.results.GetAccountResult
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordStartResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult


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
     * @return [com.microsoft.identity.nativeauth.statemachine.states.AccountState] if there is a signed in account, null otherwise.
     */
    suspend fun getCurrentAccount(): GetAccountResult

    /**
     * Retrieve the current signed in account from cache; Kotlin coroutines variant.
     *
     * @return [com.microsoft.identity.nativeauth.statemachine.states.AccountState] if there is a signed in account, null otherwise.
     */
    fun getCurrentAccount(callback: NativeAuthPublicClientApplication.GetCurrentAccountCallback)

    /**
     * Sign in a user with a given username; Kotlin coroutines variant.
     *
     * @param username username of the account to sign in.
     * @param password (Optional) password of the account to sign in.
     * @param scopes (Optional) scopes to request during the sign in.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.SignInResult] see detailed possible return state under the object.
     * @throws [MsalException] if an account is already signed in.
     */
    suspend fun signIn(username: String, password: CharArray? = null,  scopes: List<String>? = null): SignInResult

    /**
     * Sign in a user with a given username; callback variant.
     *
     * @param username username of the account to sign in.
     * @param password (Optional) password of the account to sign in.
     * @param scopes (Optional) scopes to request during the sign in.
     * @param callback [com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication.SignInCallback] to receive the result.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.SignInResult] see detailed possible return state under the object.
     * @throws [MsalException] if an account is already signed in.
     */
    fun signIn(username: String, password: CharArray? = null, scopes: List<String>? = null, callback: NativeAuthPublicClientApplication.SignInCallback)

    /**
     * Sign up the account starting from a username; Kotlin coroutines variant.
     *
     * @param username username of the account to sign up.
     * @param password (Optional) password of the account to sign up.
     * @param attributes (Optional) user attributes to be used during account creation.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.SignUpResult] see detailed possible return state under the object.
     * @throws MsalClientException if an account is already signed in.
     */
    suspend fun signUp(username: String, password: CharArray? = null, attributes: UserAttributes? = null): SignUpResult

    /**
     * Sign up the account starting from a username; callback variant.
     *
     * @param username username of the account to sign up.
     * @param password (Optional) password of the account to sign up.
     * @param attributes (Optional) user attributes to be used during account creation.
     * @param callback [com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication.SignUpCallback] to receive the result.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.SignUpResult] see detailed possible return state under the object.
     * @throws MsalClientException if an account is already signed in.
     */
    fun signUp(username: String, password: CharArray? = null, attributes: UserAttributes? = null, callback: NativeAuthPublicClientApplication.SignUpCallback)

    /**
     * Reset password for the account starting from a username; Kotlin coroutines variant.
     *
     * @param username username of the account to reset password.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordStartResult] see detailed possible return state under the object.
     * @throws MsalClientException if an account is already signed in.
     */
    suspend fun resetPassword(username: String): ResetPasswordStartResult

    /**
     * Reset password for the account starting from a username; callback variant.
     *
     * @param username username of the account to reset password.
     * @param callback [com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication.ResetPasswordCallback] to receive the result.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordStartResult] see detailed possible return state under the object.
     * @throws MsalClientException if an account is already signed in.
     */
    fun resetPassword(username: String, callback: NativeAuthPublicClientApplication.ResetPasswordCallback)
}
