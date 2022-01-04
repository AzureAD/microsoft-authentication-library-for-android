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
import com.microsoft.identity.common.java.util.TaskCompletedCallbackWithError;

import java.util.List;

/**
 * An interface that contains list of operations that are available when MSAL is in 'multiple account' mode.
 * - This mode allows an application to make API calls with more than one accounts.
 * - The application will only be able to retrieve/remove accounts that have been used to acquire token interactively in this application
 * - API calls' scope is limited to 'the calling app'. (i.e. removeAccount() will not remove credentials of the same account in other apps).
 * <p>
 * This is MSAL's default mode.
 */
public interface IMultipleAccountPublicClientApplication extends IPublicClientApplication {
    /**
     * Asynchronously returns a List of {@link IAccount} objects for which this application has RefreshTokens.
     *
     * @param callback The callback to notify once this action has finished.
     */
    void getAccounts(@NonNull final LoadAccountsCallback callback);

    /**
     * Returns a List of {@link IAccount} objects for which this application has RefreshTokens.
     */
    @WorkerThread
    List<IAccount> getAccounts() throws InterruptedException, MsalException;

    /**
     * Retrieve the IAccount object matching the identifier.
     * The identifier could be homeAccountIdentifier, localAccountIdentifier or username.
     *
     * @param identifier String of the identifier
     * @param callback   The callback to notify once this action has finished.
     */
    void getAccount(@NonNull final String identifier,
                    @NonNull final GetAccountCallback callback
    );

    /**
     * Retrieve the IAccount object matching the identifier.
     * The identifier could be homeAccountIdentifier, localAccountIdentifier or username.
     *
     * @param identifier String of the identifier
     */
    @WorkerThread
    IAccount getAccount(@NonNull final String identifier) throws InterruptedException, MsalException;

    /**
     * Removes the Account and Credentials (tokens) for the supplied IAccount.
     *
     * @param account The IAccount whose entry and associated tokens should be removed.
     */
    void removeAccount(@Nullable final IAccount account,
                       @NonNull final RemoveAccountCallback callback
    );

    /**
     * Removes the Account and Credentials (tokens) for the supplied IAccount.
     *
     * @param account The IAccount whose entry and associated tokens should be removed.
     * @return True, if the account was removed. False otherwise.
     */
    @WorkerThread
    boolean removeAccount(@Nullable final IAccount account) throws MsalException, InterruptedException;

    /**
     * Acquire token interactively, will pop-up webUI. Interactive flow will skip the cache lookup.
     *
     * @param activity  Non-null {@link Activity} that will be used as the parent activity for launching the {@link com.microsoft.identity.common.internal.providers.oauth2.AuthorizationActivity}.
     * @param scopes    The non-null array of scopes to be requested for the access token.
     *                  MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     * @param loginHint Optional. If provided, will be used as the query parameter sent for authenticating the user,
     *                  which will have the UPN pre-populated.
     * @param callback  The Non-null {@link AuthenticationCallback} to receive the result back.
     *                  1) If user cancels the flow by pressing the device back button, the result will be sent
     *                  back via {@link AuthenticationCallback#onCancel()}.
     *                  2) If the sdk successfully receives the token back, result will be sent back via
     *                  {@link AuthenticationCallback#onSuccess(IAuthenticationResult)}
     *                  3) All the other errors will be sent back via
     *                  {@link AuthenticationCallback#onError(MsalException)}.
     */
    void acquireToken(@NonNull final Activity activity,
                      @NonNull final List<String> scopes,
                      @Nullable final String loginHint,
                      @NonNull final AuthenticationCallback callback
    );

    /**
     * Acquire token interactively, will pop-up webUI. Interactive flow will skip the cache lookup.
     *
     * @param activity  Non-null {@link Activity} that will be used as the parent activity for launching the {@link com.microsoft.identity.common.internal.providers.oauth2.AuthorizationActivity}.
     * @param scopes    The non-null array of scopes to be requested for the access token.
     *                  MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     * @param loginHint Optional. If provided, will be used as the query parameter sent for authenticating the user,
     *                  which will have the UPN pre-populated.
     * @param callback  The Non-null {@link AuthenticationCallback} to receive the result back.
     *                  1) If user cancels the flow by pressing the device back button, the result will be sent
     *                  back via {@link AuthenticationCallback#onCancel()}.
     *                  2) If the sdk successfully receives the token back, result will be sent back via
     *                  {@link AuthenticationCallback#onSuccess(IAuthenticationResult)}
     *                  3) All the other errors will be sent back via
     *                  {@link AuthenticationCallback#onError(MsalException)}.
     */
    void acquireToken(@NonNull final Activity activity,
                      @NonNull final String[] scopes,
                      @Nullable final String loginHint,
                      @NonNull final AuthenticationCallback callback
    );

    /**
     * Perform acquire token silent call. If there is a valid access token in the cache, the sdk will return the access token; If
     * no valid access token exists, the sdk will try to find a refresh token and use the refresh token to get a new access token. If refresh token does not exist
     * or it fails the refresh, exception will be sent back via callback.
     *
     * @param scopes    The non-null array of scopes to be requested for the access token.
     *                  MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     * @param account   {@link IAccount} represents the account to silently request tokens for.
     * @param authority Authority to issue the token.
     */
    @WorkerThread
    IAuthenticationResult acquireTokenSilent(@NonNull final String[] scopes,
                                             @NonNull final IAccount account,
                                             @NonNull final String authority) throws MsalException, InterruptedException;

    /**
     * Perform acquire token silent call. If there is a valid access token in the cache, the sdk will return the access token; If
     * no valid access token exists, the sdk will try to find a refresh token and use the refresh token to get a new access token. If refresh token does not exist
     * or it fails the refresh, exception will be sent back via callback.
     *
     * @param scopes    The non-null array of scopes to be requested for the access token.
     *                  MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     * @param account   {@link IAccount} represents the account to silently request tokens for.
     * @param authority Authority to issue the token.
     * @param callback  {@link SilentAuthenticationCallback} that is used to send the result back. The success result will be
     *                  sent back via {@link SilentAuthenticationCallback#onSuccess(IAuthenticationResult)}.
     *                  Failure case will be sent back via {
     * @link AuthenticationCallback#onError(MsalException)}.
     */
    void acquireTokenSilentAsync(@NonNull final String[] scopes,
                                 @NonNull final IAccount account,
                                 @NonNull final String authority,
                                 @NonNull final SilentAuthenticationCallback callback);

    interface GetAccountCallback extends TaskCompletedCallbackWithError<IAccount, MsalException> {
        /**
         * Called once succeed and pass the result object.
         *
         * @param result the success result.
         */
        void onTaskCompleted(IAccount result);

        /**
         * Called once exception thrown.
         *
         * @param exception
         */
        void onError(MsalException exception);
    }

    interface RemoveAccountCallback {
        /**
         * Invoked when account successfully removed
         */
        void onRemoved();

        /**
         * Invoked when the account failed to load.
         *
         * @param exception the exception object.
         */
        void onError(@NonNull final MsalException exception);
    }
}
