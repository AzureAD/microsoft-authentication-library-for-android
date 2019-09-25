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
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.internal.controllers.TaskCompletedCallbackWithError;

import java.util.List;

public interface IPublicClientApplication {

    /**
     * Acquire token interactively, will pop-up webUI. Interactive flow will skip the cache lookup.
     * Default value for {@link UiBehavior} is {@link UiBehavior#SELECT_ACCOUNT}.
     *
     * @param activity Non-null {@link Activity} that is used as the parent activity for launching the {@link AuthenticationActivity}.
     * @param scopes   The non-null array of scopes to be requested for the access token.
     *                 MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     * @param callback The {@link AuthenticationCallback} to receive the result back.
     *                 1) If user cancels the flow by pressing the device back button, the result will be sent
     *                 back via {@link AuthenticationCallback#onCancel()}.
     *                 2) If the sdk successfully receives the token back, result will be sent back via
     *                 {@link AuthenticationCallback#onSuccess(IAuthenticationResult)}
     *                 3) All the other errors will be sent back via
     *                 {@link AuthenticationCallback#onError(MsalException)}.
     */
    void acquireToken(@NonNull final Activity activity,
                      @NonNull final String[] scopes,
                      @NonNull final AuthenticationCallback callback
    );

    /**
     * Acquire token interactively, will pop-up webUI. Interactive flow will skip the cache lookup.
     * Default value for {@link UiBehavior} is {@link UiBehavior#SELECT_ACCOUNT}.
     * <p>
     * Convey parameters via the AquireTokenParameters object
     *
     * @param acquireTokenParameters
     */
    void acquireToken(@NonNull final AcquireTokenParameters acquireTokenParameters);


    /**
     * Perform acquire token silent call. If there is a valid access token in the cache, the sdk will return the access token; If
     * no valid access token exists, the sdk will try to find a refresh token and use the refresh token to get a new access token. If refresh token does not exist
     * or it fails the refresh, exception will be sent back via callback.
     *
     * @param acquireTokenSilentParameters
     */
    void acquireTokenSilentAsync(@NonNull final AcquireTokenSilentParameters acquireTokenSilentParameters);

    /**
     * Perform acquire token silent call. If there is a valid access token in the cache, the sdk will return the access token; If
     * no valid access token exists, the sdk will try to find a refresh token and use the refresh token to get a new access token. If refresh token does not exist
     * or it fails the refresh, exception will be sent back via callback.
     *
     * @param acquireTokenSilentParameters
     */
    @WorkerThread
    IAuthenticationResult acquireTokenSilent(@NonNull final AcquireTokenSilentParameters acquireTokenSilentParameters) throws InterruptedException, MsalException;

    /**
     * Returns the PublicClientConfiguration for this instance of PublicClientApplication
     * Configuration is based on the defaults established for MSAl and can be overridden by creating the
     * PublicClientApplication using {@link PublicClientApplication#PublicClientApplication(Context, PublicClientApplicationConfiguration)}
     *
     * @return
     */
    PublicClientApplicationConfiguration getConfiguration();

    /**
     * Returns whether the application is being run on a device that is marked as a shared.
     * Only SingleAccountPublicClientApplications may be used on shared devices
     * @return
     */
    boolean isSharedDevice();

    interface LoadAccountsCallback extends TaskCompletedCallbackWithError<List<IAccount>, MsalException> {
        /**
         * Called once succeed and pass the result object.
         *
         * @param result the success result.
         */
        void onTaskCompleted(List<IAccount> result);

        /**
         * Called once exception thrown.
         *
         * @param exception
         */
        void onError(MsalException exception);
    }


    /**
     * Listener callback for asynchronous initialization of IPublicClientApplication object.
     */
    interface ApplicationCreatedListener {
        /**
         * Called once an IPublicClientApplication is successfully created.
         */
        void onCreated(final IPublicClientApplication application);

        /**
         * Called once IPublicClientApplication can't be created.
         */
        void onError(final MsalException exception);
    }


    /**
     * Listener callback for asynchronous initialization of ISingleAccountPublicClientApplication object.
     */
    interface ISingleAccountApplicationCreatedListener {
        /**
         * Called once an ISingleAccountPublicClientApplication is successfully created.
         */
        void onCreated(final ISingleAccountPublicClientApplication application);

        /**
         * Called once ISingleAccountPublicClientApplication can't be created.
         */
        void onError(final MsalException exception);
    }

    /**
     * Listener callback for asynchronous initialization of IMultipleAccountPublicClientApplication object.
     */
    interface IMultipleAccountApplicationCreatedListener {
        /**
         * Called once an IMultipleAccountPublicClientApplication is successfully created.
         */
        void onCreated(final IMultipleAccountPublicClientApplication application);

        /**
         * Called once IMultipleAccountPublicClientApplication can't be created.
         */
        void onError(final MsalException exception);
    }

}
