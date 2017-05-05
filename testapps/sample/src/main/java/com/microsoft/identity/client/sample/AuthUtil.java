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

package com.microsoft.identity.client.sample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.MsalException;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.UiBehavior;
import com.microsoft.identity.client.User;

import java.util.ArrayList;
import java.util.List;

final class AuthUtil {

    private static final String TAG = AuthUtil.class.getSimpleName();
    private static final Object LOCK = new Object();
    // There should be only one instance of PublicClientApplication per app
    private PublicClientApplication mApplication = null;
    private static final String[] SCOPES = {"User.Read"};
    private static final String[] EXTRA_SCOPES = {"Calendars.Read"};
    private static final String CLIENT_ID = "9851987a-55e5-46e2-8d70-75f8dc060f21";
    private List<User> mUsers = new ArrayList<>();

    public AuthUtil(final Context context) {
        if (mApplication == null) {
            mApplication = new PublicClientApplication(context, CLIENT_ID);
        }
    }

    // Get the number of users signed into the app. In the context of the current app this method can return 1 or 0
    int getUserCount() {
        getUsers();
        return mUsers.size();
    }

    // Do an interactive login request
    // Task is an handle to MainActivity which has overrriden useAccessToken and onRequestFailure
    // On acquireToken success, useAccessToken is called with the AccessToken and on failure onRequestFailure is called
    // with the exception
    void doAcquireToken(final AuthenticatedTask task, final Activity activity) {
        // Interactive request must not be called from more than one thread at a time.
        synchronized (LOCK) {
            AuthCallback callback = new AuthCallback(task);
            mApplication.acquireToken(activity, SCOPES, callback);
        }
    }

    // Do an silent login request
    // Task is an handle to MainActivity which has overrriden useAccessToken and onRequestFailure
    // On acquireToken success, useAccessToken is called with the AccessToken and on failure onRequestFailure is called
    // with the exception
    void doAcquireTokenSilent(final AuthenticatedTask task) {
        AuthCallback callback = new AuthCallback(task);
        mApplication.acquireTokenSilentAsync(SCOPES, mUsers.get(0), callback);
    }

    // Remove the user from the list of users being tracked by the application. This does not clear the cookies
    void doSignout() {
        final int userCount = getUserCount();
        for (int i = 0; i < userCount; i++) {
            mApplication.remove(mUsers.get(i));
        }
    }

    // This is a shim, so MainActivity's onActivityResult can feed the result back to the PublicClientApplication's
    // handleInteractiveRequestRedirect
    // Any special handling of request or result code that the developer wants to do must be done here.
    void handleInteractiveRequestRedirect(int requestCode, int resultCode, Intent data) {
        mApplication.handleInteractiveRequestRedirect(requestCode, resultCode, data);
    }

    // This function just demonstrates how to request more scopes.
    // The app does nothing with the newly acquired scopes yet.
    void doExtraScopeRequest(final Activity activity) {
        getUsers();
        mApplication.acquireToken(activity,
                SCOPES,
                mUsers.get(0), // The user object
                UiBehavior.CONSENT,
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

    private static final class AuthCallback implements AuthenticationCallback {

        private AuthenticationResult mAuthResult = null;
        private AuthenticatedTask mTask = null;

        public AuthCallback(AuthenticatedTask task) {
            mTask = task;
        }

        @Override
        public void onSuccess(AuthenticationResult authenticationResult) {
            mAuthResult = authenticationResult;
            mTask.useAccessToken(mAuthResult.getAccessToken());
        }

        @Override
        public void onError(MsalException exception) {
            mTask.onRequestFailure(exception);
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
