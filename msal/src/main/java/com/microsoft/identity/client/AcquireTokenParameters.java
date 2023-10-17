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

import androidx.fragment.app.Fragment;

import com.microsoft.identity.common.java.ui.PreferredAuthMethod;

import java.util.List;
import java.util.Map;

/**
 * Encapsulates the parameters passed to the acquireToken methods of PublicClientApplication
 */
public class AcquireTokenParameters extends TokenParameters {

    private Activity mActivity;
    private Fragment mFragment;
    private String mLoginHint;
    private Prompt mPrompt;
    private List<String> mExtraScopesToConsent;
    private List<Map.Entry<String, String>> mExtraQueryStringParameters;
    private AuthenticationCallback mCallback;

    private PreferredAuthMethod mPreferredAuthMethod;

    public AcquireTokenParameters(AcquireTokenParameters.Builder builder) {
        super(builder);
        mActivity = builder.mActivity;
        mFragment = builder.mFragment;
        mLoginHint = builder.mLoginHint;
        mPreferredAuthMethod = builder.mPreferredAuthMethod;
        mPrompt = builder.mPrompt;
        mExtraScopesToConsent = builder.mExtraScopesToConsent;
        mExtraQueryStringParameters = builder.mExtraQueryStringParameters;
        mCallback = builder.mCallback;
    }

    /**
     * Non-null {@link Activity} that will be used as the parent activity for launching the {@link com.microsoft.identity.common.internal.providers.oauth2.AuthorizationActivity}
     */
    public Activity getActivity() {
        return mActivity;
    }

    /**
     * Optional {@link Fragment} that will be replaced by {@link com.microsoft.identity.common.internal.providers.oauth2.AuthorizationFragment}
     */
    public Fragment getFragment() {
        return mFragment;
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
    void setLoginHint(String loginHint) {
        this.mLoginHint = loginHint;
    }

    public PreferredAuthMethod getPreferredAuthMethod() {
        return mPreferredAuthMethod;
    }

    /**
     * Controls the value of the prompt parameter sent along with the authorization request.
     *
     * @return
     */
    public Prompt getPrompt() {
        return mPrompt;
    }

    /**
     * These are additional scopes (of other resources) that you would like the user to authorize up front.
     * (MSAL's {@link TokenParameters#getScopes()} parameter should only contain scopes for a single resource
     *
     * https://docs.microsoft.com/bs-latn-ba/azure/active-directory/develop/msal-net-user-gets-consent-for-multiple-resources
     *
     * @return
     */
    public List<String> getExtraScopesToConsent() {
        return mExtraScopesToConsent;
    }

    /**
     * If you've been instructed to pass additional query string parameters to the authorization endpoint.  You can get these here.
     * Otherwise... would recommend not touching.
     *
     * @return
     */
    public List<Map.Entry<String, String>> getExtraQueryStringParameters() {
        return mExtraQueryStringParameters;
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

    public static class Builder extends TokenParameters.Builder<AcquireTokenParameters.Builder> {

        private Activity mActivity;
        private Fragment mFragment;
        private String mLoginHint;

        private PreferredAuthMethod mPreferredAuthMethod;
        private Prompt mPrompt;
        private List<String> mExtraScopesToConsent;
        private List<Map.Entry<String, String>> mExtraQueryStringParameters;
        private AuthenticationCallback mCallback;

        public AcquireTokenParameters.Builder startAuthorizationFromActivity(final Activity activity) {
            mActivity = activity;
            return self();
        }

        public AcquireTokenParameters.Builder withFragment(Fragment fragment) {
            mFragment = fragment;
            return self();
        }

        public AcquireTokenParameters.Builder withLoginHint(String loginHint) {
            mLoginHint = loginHint;
            return self();
        }

        public AcquireTokenParameters.Builder withPreferredAuthMethod(PreferredAuthMethod preferredAuthMethod) {
            mPreferredAuthMethod = preferredAuthMethod;
            return self();
        }


        public AcquireTokenParameters.Builder withPrompt(Prompt prompt) {
            mPrompt = prompt;
            return self();
        }

        public AcquireTokenParameters.Builder withOtherScopesToAuthorize(List<String> scopes) {
            mExtraScopesToConsent = scopes;
            return self();
        }

        public AcquireTokenParameters.Builder withAuthorizationQueryStringParameters(
                List<Map.Entry<String, String>> parameters) {
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
