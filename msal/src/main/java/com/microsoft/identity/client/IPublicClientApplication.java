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

import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.java.util.TaskCompletedCallbackWithError;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface IPublicClientApplication {

    /**
     * @deprecated  This method is now deprecated. The library is moving towards standardizing the use of TokenParameter subclasses as the
     *              parameters for the API. Use {@link IPublicClientApplication#acquireToken(AcquireTokenParameters)} instead.
     *
     * Acquire token interactively, will pop-up webUI. Interactive flow will skip the cache lookup.
     * Default value for {@link Prompt} is {@link Prompt#SELECT_ACCOUNT}.
     *
     * @param activity Non-null {@link Activity} that is used as the parent activity for launching the {@link com.microsoft.identity.common.internal.providers.oauth2.AuthorizationActivity}.
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
    @Deprecated
    void acquireToken(@NonNull final Activity activity,
                      @NonNull final String[] scopes,
                      @NonNull final AuthenticationCallback callback
    );

    /**
     * Acquire token interactively, will pop-up webUI. Interactive flow will skip the cache lookup.
     * Default value for {@link Prompt} is {@link Prompt#SELECT_ACCOUNT}.
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

//    /**
//     * Perform the Device Code Flow (DCF) protocol to allow a device without input capability to authenticate and get a new access token.
//     * This flow is now supported in Broker as well. It also supports requesting Claims using the "claims" Request. Parameter.
//     *
//     * @param scopes   the desired access scopes
//     * @param callback callback object used to communicate with the API throughout the protocol
//     * @param claims claims Authentication Request parameter requests that specific Claims be returned from the UserInfo Endpoint and/or in the ID Token.
//     * @param correlationId  correlation id of this request
//     *
//     * Important: Use of this API requires setting the minimum_required_broker_protocol_version to
//     * "13.0" or higher.
//     * Note: This API is in testing phase and might return not supported error until fully supported.
//     */
//    void acquireTokenWithDeviceCode(@NonNull List<String> scopes, @NonNull final DeviceCodeFlowCallback callback, @Nullable final ClaimsRequest claims, @Nullable final UUID correlationId);

    /**
     * Perform the Device Code Flow (DCF) protocol to allow a device without input capability to authenticate and get a new access token.
     * Currently, flow is only supported in local MSAL. No Broker support.
     *
     * @param scopes   the desired access scopes
     * @param callback callback object used to communicate with the API throughout the protocol
     */
    void acquireTokenWithDeviceCode(@NonNull List<String> scopes, @NonNull final DeviceCodeFlowCallback callback);

    /**
     * @deprecated  This method is now deprecated. The library is moving away from using an array for scopes.
     *              Use {@link IPublicClientApplication#acquireTokenWithDeviceCode(List, DeviceCodeFlowCallback)} instead.
     *
     * Perform the Device Code Flow (DCF) protocol to allow a device without input capability to authenticate and get a new access token.
     * Currently, flow is only supported in local MSAL. No Broker support.
     *
     * @param scopes   the desired access scopes
     * @param callback callback object used to communicate with the API throughout the protocol
     */
    @Deprecated
    void acquireTokenWithDeviceCode(@NonNull String[] scopes, @NonNull final DeviceCodeFlowCallback callback);

    /**
     * Returns the PublicClientConfiguration for this instance of PublicClientApplication.
     *
     * @return The PublicClientApplicationConfiguration.
     */
    PublicClientApplicationConfiguration getConfiguration();

    /**
     * Returns whether the application is being run on a device that is marked as a shared.
     * Only SingleAccountPublicClientApplications may be used on shared devices
     *
     * @return
     */
    boolean isSharedDevice();

    /**
     * Signs the provided {@link PoPAuthenticationScheme} parameters into a JWT on behalf of the
     * provided {@link IAccount}.
     * <p>
     * Important: Use of this API requires setting the minimum_required_broker_protocol_version to
     * "6.0" or higher.
     *
     * @param account       The account for whom signing shall occur.
     * @param popParameters The input parameters.
     * @return The resulting SHR.
     */
    @NonNull
    String generateSignedHttpRequest(@NonNull final IAccount account,
                                     @NonNull final PoPAuthenticationScheme popParameters
    ) throws MsalException;

    /**
     * Signs the provided {@link PoPAuthenticationScheme} parameters into a JWT on behalf of the
     * provided {@link IAccount}.
     * <p>
     * Important: Use of this API requires setting the minimum_required_broker_protocol_version to
     * "6.0" or higher.
     *
     * @param account       The account for whom signing shall occur.
     * @param popParameters The input parameters.
     * @param callback      The callback object to receive the result (or error).
     * @return The resulting SHR.
     */
    void generateSignedHttpRequest(@NonNull final IAccount account,
                                   @NonNull final PoPAuthenticationScheme popParameters,
                                   @NonNull final SignedHttpRequestRequestCallback callback
    );

    /**
     * Callback used to receive the result of {@link #generateSignedHttpRequest(IAccount, PoPAuthenticationScheme)}.
     */
    interface SignedHttpRequestRequestCallback extends TaskCompletedCallbackWithError<String, MsalException> {

        /**
         * Called after signing of the supplied properties has finished.
         *
         * @param result The resulting SHR.
         */
        void onTaskCompleted(String result);

        /**
         * Called if an error occurs during signing.
         *
         * @param exception
         */
        void onError(MsalException exception);
    }

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

    /**
     * Callback object used in Device Code Flow.
     * This callback provides the following methods for communicating with the protocol.
     * 1). Receiving authentication information (user_code, verification_uri, and instruction message)
     * via {@link DeviceCodeFlowCallback#onUserCodeReceived(String, String, String, Date)}.
     * 2). Receiving a successful authentication result containing a fresh access token
     * via {@link DeviceCodeFlowCallback#onTokenReceived(IAuthenticationResult)}.
     * 3). Receiving an exception detailing what went wrong in the protocol
     * via {@link DeviceCodeFlowCallback#onError(MsalException)}.
     * <p>
     * Refer to {@link PublicClientApplication#acquireTokenWithDeviceCode(List, DeviceCodeFlowCallback)}.
     */
    interface DeviceCodeFlowCallback {
        /**
         * Invoked to display verification uri, user code, and instruction message during device code flow.
         *
         * @param vUri                  verification uri
         * @param userCode              user code
         * @param message               instruction message
         * @param sessionExpirationDate the expiration date of DCF session to be displayed to the user ONLY.
         *                              When the session expires, onError() will return an exception with DEVICE_CODE_FLOW_EXPIRED_TOKEN_ERROR_CODE.
         *                              Please rely on that exception for non-UX purposes.
         */
        void onUserCodeReceived(@NonNull final String vUri,
                                @NonNull final String userCode,
                                @NonNull final String message,
                                @NonNull final Date sessionExpirationDate);

        /**
         * Invoked once token is received and passes the {@link AuthenticationResult} object.
         *
         * @param authResult the authentication result
         */
        void onTokenReceived(@NonNull final IAuthenticationResult authResult);

        /**
         * Invoked if an error is encountered during the device code flow and passes the exception object.
         *
         * @param exception error exception
         */
        void onError(@NonNull final MsalException exception);
    }

}
