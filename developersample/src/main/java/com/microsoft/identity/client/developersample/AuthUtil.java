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

package com.microsoft.identity.client.developersample;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.ILoggerCallback;
import com.microsoft.identity.client.Logger;
import com.microsoft.identity.client.MsalClientException;
import com.microsoft.identity.client.MsalException;
import com.microsoft.identity.client.MsalServiceException;
import com.microsoft.identity.client.MsalUiRequiredException;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.UiBehavior;
import com.microsoft.identity.client.User;

import java.util.List;

final class AuthUtil {

    private static final String TAG = AuthUtil.class.getSimpleName();
    private static final Object LOCK = new Object();
    // There should be only one instance of PublicClientApplication per app
    private PublicClientApplication mApplication = null;
    private static final String[] SCOPES = {"https://graph.microsoft.com/User.Read"};
    private static final String[] EXTRA_SCOPES = {"Calendars.Read"};
    private static final String CLIENT_ID = "9851987a-55e5-46e2-8d70-75f8dc060f21";
    private List<User> mUsers = null;
    final Activity mActivity;

    /**
     * When initializing the {@link PublicClientApplication}, all the apps should only provide us the application context instead of
     * the running activity itself. If running activity itself is provided, that will have the sdk hold a strong reference of the activity
     * which could potentially cause the object not correctly garbage collected and cause activity leak.
     * <p>
     * External Logger should be provided by the Calling app. The sdk logs to the logcat by default, and loglevel is enabled at verbose level.
     * To set external logger,
     * {@link Logger#setExternalLogger(ILoggerCallback)}.
     * To set log level,
     * {@link Logger#setLogLevel(Logger.LogLevel)}
     * By default, the sdk won't give back any Pii logging. However the app can turn it on, this is up to the application's privacy policy.
     * To turn on the Pii logging,
     * {@link Logger#setEnablePII(boolean)}
     * Application can also set the component name. There are cases that other sdks will also take dependency on MSAL i.e. microsoft graph sdk,
     * providing the component name will help separate the logs from application and the logs from the sdk running inside of
     * the apps.
     * To set component name:
     * {@link PublicClientApplication#setComponent(String)}
     * <p>
     * For the {@link AuthenticationCallback}, MSAL exposes three results 1) Success, which contains the {@link AuthenticationResult} 2) Failure case,
     * which contains {@link MsalException} and 3) Cancel, specifically for user canceling the flow.
     * <p>
     * For the failure case, MSAL exposes three sub exceptions:
     * 1) {@link MsalClientException}, which is specifically for the exceptions running inside the client app itself, could be no active network,
     * Json parsing failure, etc.
     * 2) {@link MsalServiceException}, which is the error that the sdk gets back when communicating to the service, could be oauth2 errors, socket timout
     * or 500/503/504. For oauth2 erros, MSAL returns back the exact error that server returns back to the sdk.
     * 3) {@link MsalUiRequiredException}, which means that UI is required.
     */
    public AuthUtil(final Activity activity) {
        if (mApplication == null) {
            mApplication = new PublicClientApplication(activity.getApplicationContext(), CLIENT_ID);
        }
        mActivity = activity;
    }

    // Get the number of users signed into the app. In the context of the current app this method can return 1 or 0
    int getUserCount() {
        getUsers();
        if (mUsers == null) {
            return 0;
        }

        return mUsers.size();
    }

    // Do an interactive login request
    // Task is an handle to MainActivity which has overrriden useAccessToken and onRequestFailure
    // On acquireToken success, useAccessToken is called with the AccessToken and on failure onRequestFailure is called
    // with the exception
    void doAcquireToken(final AuthenticatedTask task) {
        // Interactive request must not be called from more than one thread at a time.
        synchronized (LOCK) {
            AuthCallback callback = new AuthCallback(task, true);
            mApplication.acquireToken(mActivity, SCOPES, callback);
        }
    }

    // Do an silent login request
    // Task is an handle to MainActivity which has overrriden useAccessToken and onRequestFailure
    // On acquireToken success, useAccessToken is called with the AccessToken and on failure onRequestFailure is called
    // with the exception
    void doAcquireTokenSilent(final AuthenticatedTask task) {
        AuthCallback callback = new AuthCallback(task, false);
        mApplication.acquireTokenSilentAsync(SCOPES, mUsers.get(0), callback);
    }

    // Remove the user from the list of users being tracked by the application. This does not clear the cookies
    void doSignout() {
        if (getUserCount() > 0) {
            mApplication.remove(mUsers.get(0));
        }
    }

    // This is a shim, so MainActivity's onActivityResult can feed the result back to the PublicClientApplication's
    // handleInteractiveRequestRedirect
    // Any special handling of request or result code that the developer wants to do must be done here.
    void doCallback(int requestCode, int resultCode, Intent data) {
        mApplication.handleInteractiveRequestRedirect(requestCode, resultCode, data);
    }

    // This function just demonstrates how to request more scopes.
    // The app does nothing with the newly acquired scopes yet.
    void doExtraScopeRequest() {
        mApplication.acquireToken(mActivity,
                SCOPES,
                "", // LoginHint
                UiBehavior.SELECT_ACCOUNT,
                "", // Extra query parameters
                EXTRA_SCOPES,
                "", // Authority
                new AuthenticationCallback() {
                    @Override
                    public void onSuccess(AuthenticationResult authenticationResult) {
                        // Not filled in for the sample app, ideally the callback should be passed and action taken on success
                    }

                    @Override
                    public void onError(MsalException exception) {
                        // Not filled in for the sample app, ideally the callback should be passed and error handled
                    }

                    @Override
                    public void onCancel() {
                        // Not filled in for the sample app
                    }
                });
    }

    // Get the list of users signed into the app. This app is designed to be a single user app, but if an app wants to
    // handle more than one user, this is the way to get the list of users who are signed in.
    private void getUsers() {
        try {
            mUsers = mApplication.getUsers();
        } catch (final MsalException exc) {
            Log.e(TAG, "Exception when getting users", exc);
        }
    }

    class AuthCallback implements AuthenticationCallback {

        private AuthenticationResult mAuthResult = null;
        private AuthenticatedTask mTask = null;
        private boolean mIsSilentRequest;

        public AuthCallback(AuthenticatedTask task, boolean isSilentRequest) {
            mTask = task;
            mIsSilentRequest = isSilentRequest;
        }

        @Override
        public void onSuccess(AuthenticationResult authenticationResult) {
            mAuthResult = authenticationResult;
            mTask.useAccessToken(mAuthResult.getAccessToken());
        }

        @Override
        public void onError(MsalException exception) {
            if (mIsSilentRequest && exception instanceof MsalUiRequiredException) {
                // This explicitly indicates that developer needs to prompt the user, it could be refresh token is expired, revoked
                // or user changes the password; or it could be that no token was found in the token cache.
                doAcquireToken(mTask);
            } else {
                mTask.onRequestFailure(exception);
            }
        }

        @Override
        public void onCancel() {
            // Intentionally left blank for sample app,
            // in real world the app needs to handle what to do if a request is cancelled
        }
    }

    interface AuthenticatedTask {
        void useAccessToken(final String accessToken);

        void onRequestFailure(final MsalException exception);
    }
}
