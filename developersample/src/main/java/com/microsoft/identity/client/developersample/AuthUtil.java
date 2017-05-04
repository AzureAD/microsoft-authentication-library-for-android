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
import com.microsoft.identity.client.MsalException;
import com.microsoft.identity.client.MsalUiRequiredException;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.User;

import java.util.List;

final class AuthUtil {

    private static final String TAG = AuthUtil.class.getSimpleName();
    private static final Object LOCK = new Object();
    private static PublicClientApplication mApplication = null;
    private static final String[] SCOPES = {"https://graph.microsoft.com/User.Read"};
    private static final String CLIENT_ID = "9851987a-55e5-46e2-8d70-75f8dc060f21";
    private List<User> mUsers = null;
    final Activity mActivity;

    public AuthUtil(final Activity activity) {
        if (mApplication == null) {
            mApplication = new PublicClientApplication(activity.getApplicationContext(), CLIENT_ID);
        }
        mActivity = activity;
    }

    private void getUsers() {
        try {
            mUsers = mApplication.getUsers();
        } catch (final MsalException exc) {
            Log.e(TAG, "Exception when getting users", exc);
        }
    }

    int getUserCount() {
        getUsers();
        if (mUsers == null) {
            return 0;
        }

        return mUsers.size();
    }

    void doAcquireToken(final AuthenticatedTask task) {
        synchronized (LOCK) {
            AuthCallback callback = new AuthCallback(task, true);
            mApplication.acquireToken(mActivity, SCOPES, callback);
        }
    }

    void doAcquireTokenSilent(final AuthenticatedTask task) {
        if (getUserCount() == 0) {
            return;
        }

        AuthCallback callback = new AuthCallback(task, false);
        mApplication.acquireTokenSilentAsync(SCOPES, mUsers.get(0), callback);
    }

    void doSignout() {
        if (getUserCount() > 0) {
            mApplication.remove(mUsers.get(0));
        }
    }

    void doCallback(int requestCode, int resultCode, Intent data) {
        mApplication.handleInteractiveRequestRedirect(requestCode, resultCode, data);
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

        }
    }

    interface AuthenticatedTask {
        void useAccessToken(final String accessToken);

        void onRequestFailure(final MsalException exception);
    }
}
