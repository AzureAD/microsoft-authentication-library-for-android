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

package com.microsoft.identity.client;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.microsoft.identity.client.exception.MsalException;

/**
 * An interface that contains list of operations that are available when MSAL is in 'single account' mode.
 * - In this mode, one account can be signed-in to the app.
 * - If the user wants to acquire a token for another account, the previous account must be signed out first.
 * <p>
 * When the device is registered as 'shared', this will be the only available PublicClientApplication the app can obtain.
 * The calling app has to support ISingleAccountPublicClientApplication if it is planning to support shared device mode.
 * <p>
 * In the shared device mode,
 * - 'Sign-in' means that the user will be signed in to the device - not just this app.
 * - Once an account is 'signed-in', every MSAL app on the device that support shared device mode will be able to retrieve this account, and use them to silently perform API calls.
 * - 'Sign-out' means that user will be signed out from the device - every MSAL apps and the default browser.
 */
public interface ISingleAccountPublicClientApplication extends IPublicClientApplication {

    /**
     * Gets the current account and notify if the current account changes.
     * This method must be called whenever the application is resumed or prior to running a scheduled background operation.
     *
     * @param callback a callback to be invoked when the operation finishes.
     */
    void getCurrentAccountAsync(final CurrentAccountCallback callback);

    /**
     * Gets the current account and notify if the current account changes.
     * This method must be called whenever the application is resumed or prior to running a scheduled background operation.
     *
     * @return CurrentAccountResult
     */
    @WorkerThread
    ICurrentAccountResult getCurrentAccount() throws InterruptedException, MsalException;

    /**
     * Allows a user to sign in to your application with one of their accounts.
     * <p>
     * Note: The authority used to make the sign in request will be either the MSAL default: https://login.microsoftonline.com/common
     * or the default authority specified by you in your configuration
     *
     * @param activity  Non-null {@link Activity} that is used as the parent activity for launching the {@link AuthenticationActivity}.
     * @param loginHint Optional. If provided, will be used as the query parameter sent for authenticating the user,
     *                  which will have the UPN pre-populated.
     * @param scopes    The non-null array of scopes to be consented to during sign in.
     *                  MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     *                  The access token returned is for MS Graph and will allow you to query for additional information about the signed in account.
     * @param callback  {@link AuthenticationCallback} that is used to send the result back. The success result will be
     *                  sent back via {@link AuthenticationCallback#onSuccess(IAuthenticationResult)}.
     *                  Failure case will be sent back via {
     * @link AuthenticationCallback#onError(MsalException)}.
     */
    void signIn(@NonNull final Activity activity,
                @NonNull final String loginHint,
                @NonNull final String[] scopes,
                @NonNull final AuthenticationCallback callback);

    /**
     * Signs out the current the Account and Credentials (tokens).
     * NOTE: If a device is marked as a shared device within broker signout will be device wide.
     *
     * @param callback a callback to be invoked when the operation finishes.
     */
    void signOut(@NonNull final SignOutCallback callback);

    /**
     * Signs out the current the Account and Credentials (tokens).
     * NOTE: If a device is marked as a shared device within broker signout will be device wide.
     *
     * @return boolean indicating whether the account was removed successfully
     */
    @WorkerThread
    boolean signOut() throws MsalException, InterruptedException;

    /**
     * Perform acquire token silent call. If there is a valid access token in the cache, the sdk will return the access token; If
     * no valid access token exists, the sdk will try to find a refresh token and use the refresh token to get a new access token. If refresh token does not exist
     * or it fails the refresh, exception will be sent back via callback.
     *
     * @param scopes    The non-null array of scopes to be requested for the access token.
     *                  MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     * @param authority Authority to issue the token.
     * @param callback  {@link SilentAuthenticationCallback} that is used to send the result back. The success result will be
     *                  sent back via {@link SilentAuthenticationCallback#onSuccess(IAuthenticationResult)}.
     *                  Failure case will be sent back via {
     * @link AuthenticationCallback#onError(MsalException)}.
     */
    void acquireTokenSilentAsync(@NonNull final String[] scopes,
                                 @NonNull final String authority,
                                 @NonNull final SilentAuthenticationCallback callback);

    /**
     * Perform acquire token silent call. If there is a valid access token in the cache, the sdk will return the access token; If
     * no valid access token exists, the sdk will try to find a refresh token and use the refresh token to get a new access token. If refresh token does not exist
     * or it fails the refresh, exception will be sent back via callback.
     *
     * @param scopes    The non-null array of scopes to be requested for the access token.
     *                  MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     * @param authority Optional. Can be passed to override the configured authority.
     */
    @WorkerThread
    IAuthenticationResult acquireTokenSilent(@NonNull final String[] scopes,
                                             @Nullable final String authority) throws MsalException, InterruptedException;

    /**
     * Callback for asynchronous loading of the msal IAccount account.
     */
    interface CurrentAccountCallback {
        /**
         * Invoked when the account is loaded.
         *
         * @param activeAccount the signed-in account. This could be nil.
         */
        void onAccountLoaded(@Nullable final IAccount activeAccount);

        /**
         * Invoked when signed-in account is changed after the application resumes, or prior to running a scheduled background operation.
         * The calling app is responsible for keeping track of this account and cleaning its states if the account changes.
         *
         * @param priorAccount   the previous signed-in account. This could be nil.
         * @param currentAccount the current signed-in account. This could be nil.
         */
        void onAccountChanged(@Nullable final IAccount priorAccount, @Nullable final IAccount currentAccount);

        /**
         * Invoked when the account failed to load.
         *
         * @param exception the exception object.
         */
        void onError(@NonNull final MsalException exception);
    }

    interface SignOutCallback {
        /**
         * Invoked when account successfully signed out
         */
        void onSignOut();

        /**
         * Invoked when the account failed to load.
         *
         * @param exception the exception object.
         */
        void onError(@NonNull final MsalException exception);
    }
}
