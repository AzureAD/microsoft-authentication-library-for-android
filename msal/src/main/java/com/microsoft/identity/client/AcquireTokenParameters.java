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
import android.util.Pair;

import java.util.List;

/**
 * Encapsulates the parameters passed to the acquireToken methods of PublicClientApplication
 */
public class AcquireTokenParameters extends TokenParameters {

    private Activity mActivity;
    private String mLoginHint;
    private UiBehavior mUiBehavior;
    private List<String> mExtraScopesToConsent;
    private List<Pair<String, String>> mExtraQueryStringParameters;
    private AuthenticationCallback mCallback;

    public AcquireTokenParameters(AcquireTokenParameters.Builder builder) {
        super(builder);
        mActivity = builder.mActivity;
        mLoginHint = builder.mLoginHint;
        mUiBehavior = builder.mUiBehavior;
        mExtraScopesToConsent = builder.mExtraScopesToConsent;
        mExtraQueryStringParameters = builder.mExtraQueryStringParameters;
        mCallback = builder.mCallback;
    }

    /**
     * Non-null {@link Activity} that will be used as the parent activity for launching the {@link AuthenticationActivity}
     *
     * @return
     */
    public Activity getActivity() {
        return mActivity;
    }

    /**
     * Non-null {@link Activity} that will be used as the parent activity for launching the {@link AuthenticationActivity}
     *
     * @param activity
     */
    public void setActivity(Activity activity) {
        this.mActivity = activity;
    }

    /**
     * Optional. Gets the login hint sent along with the authorization request.
     *
     * @return
     */
    public String getLoginHint() {
        return mLoginHint;
    }

    /**
     * Sets the login hint sent along with the authorization request.
     *
     * @param loginHint
     */
    public void setLoginHint(String loginHint) {
        this.mLoginHint = loginHint;
    }

    /**
     * Controls the value of the prompt parameter sent along with the authorization request.
     *
     * @return
     */
    public UiBehavior getUiBehavior() {
        return mUiBehavior;
    }

    /**
     * Controls the value of the prompt parameter sent along with the authorization request.
     *
     * @param uiBehavior
     */
    public void setUiBehavior(UiBehavior uiBehavior) {
        this.mUiBehavior = uiBehavior;
    }

    /**
     * These are additional scopes that you would like the user to authorize the use of, while getting consent
     * for the first set of scopes
     *
     * @return
     */
    public List<String> getExtraScopesToConsent() {
        return mExtraScopesToConsent;
    }

    /**
     * These are additional scopes that you would like the user to authorize the use of, while getting consent
     * for the first set of scopes
     *
     * @param extraScopesToConsent
     */
    public void setExtraScopesToConsent(List<String> extraScopesToConsent) {
        this.mExtraScopesToConsent = extraScopesToConsent;
    }

    /**
     * If you've been instructed to pass additional query string parameters to the authorization endpoint.  You can get these here.
     * Otherwise... would recommend not touching.
     *
     * @return
     */
    public List<Pair<String, String>> getExtraQueryStringParameters() {
        return mExtraQueryStringParameters;
    }

    /**
     * If you've been instructed to pass additional query string parameters to the authorization endpoint.  You can set these here.
     * Otherwise... would recommend not touching.
     *
     * @param extraQueryStringParameters
     */
    public void setExtraQueryStringParameters(List<Pair<String, String>> extraQueryStringParameters) {
        this.mExtraQueryStringParameters = extraQueryStringParameters;
    }

    /**
     * The Non-null {@link AuthenticationCallback} to receive the result back.
     * 1) If user cancels the flow by pressing the device back button, the result will be sent
     * back via {@link AuthenticationCallback#onCancel()}.
     * 2) If the sdk successfully receives the token back, result will be sent back via
     * {@link AuthenticationCallback#onSuccess(IAuthenticationResult)}
     * 3) All the other errors will be sent back via
     * {@link AuthenticationCallback#onError(com.microsoft.identity.client.exception.MsalException)}.
     *
     * @return
     */
    public AuthenticationCallback getCallback() {
        return mCallback;
    }

    /**
     * The Non-null {@link AuthenticationCallback} to receive the result back.
     * 1) If user cancels the flow by pressing the device back button, the result will be sent
     * back via {@link AuthenticationCallback#onCancel()}.
     * 2) If the sdk successfully receives the token back, result will be sent back via
     * {@link AuthenticationCallback#onSuccess(IAuthenticationResult)}
     * 3) All the other errors will be sent back via
     * {@link AuthenticationCallback#onError(com.microsoft.identity.client.exception.MsalException)}.
     *
     * @param callback
     */
    public void setCallback(AuthenticationCallback callback) {
        this.mCallback = callback;
    }

    public static class Builder extends TokenParameters.Builder<AcquireTokenParameters.Builder> {

        private Activity mActivity;
        private String mLoginHint;
        private UiBehavior mUiBehavior;
        private List<String> mExtraScopesToConsent;
        private List<Pair<String, String>> mExtraQueryStringParameters;
        private AuthenticationCallback mCallback;

        public AcquireTokenParameters.Builder startAuthorizationFromActivity(Activity activity) {
            mActivity = activity;
            return self();
        }

        public AcquireTokenParameters.Builder withLoginHint(String loginHint) {
            mLoginHint = loginHint;
            return self();
        }

        public AcquireTokenParameters.Builder withUiBehavior(UiBehavior uiBehavior) {
            mUiBehavior = uiBehavior;
            return self();
        }

        public AcquireTokenParameters.Builder withOtherScopesToAuthorize(List<String> scopes) {
            mExtraScopesToConsent = scopes;
            return self();
        }

        public AcquireTokenParameters.Builder withAuthorizationQueryStringParameters(
                List<Pair<String, String>> parameters) {
            mExtraQueryStringParameters = parameters;
            return self();
        }

        public AcquireTokenParameters.Builder withCallback(
                final AuthenticationCallback authenticationCallback) {
            mCallback = authenticationCallback;
            return self();
        }

        @Override
        public AcquireTokenParameters.Builder self() {
            return this;
        }

        public AcquireTokenParameters build() {
            return new AcquireTokenParameters(this);
        }
    }

}
