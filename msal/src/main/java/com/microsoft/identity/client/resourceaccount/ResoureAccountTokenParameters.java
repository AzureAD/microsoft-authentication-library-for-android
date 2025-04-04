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

package com.microsoft.identity.client.resourceaccount;

import android.app.Activity;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.TokenParameters;
import com.microsoft.identity.common.java.ui.PreferredAuthMethod;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * Encapsulates the parameters passed to the acquireToken methods of PublicClientApplication
 */
@Getter
@Accessors(prefix = "m")
public class ResoureAccountTokenParameters extends TokenParameters {

    /**
     * Resource account's home account id. This is a required field.
     * This is primary field that used to identify the resource account and communicate to backend services
     * like ADRS.
     */
    @NonNull
    private final String mHomeAccountId;

    /**
     * The AAD device id associated with this resource account. If provided
     * this can be used validate against the actual deviceId stored in broker for given homeAccountId
     */
    @Nullable
    private final String mAadDeviceId;

    /**
     * The login hint to be used for the resource account. This is optional can help for account lookups.
     */
    @Nullable
    private final String mLoginHint;

    /**
     * Call back receive result success or failure result.
     */
    @Nullable
    private final SilentAuthenticationCallback mCallback;

    public ResoureAccountTokenParameters(ResoureAccountTokenParameters.Builder builder) {
        super(builder);
        mHomeAccountId = builder.mHomeAccountId;
        mAadDeviceId = builder.mAadDeviceId;
        mLoginHint = builder.mLoginHint;
        mCallback = builder.mCallback;
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
    public SilentAuthenticationCallback getCallback() {
        return mCallback;
    }

    public static class Builder extends TokenParameters.Builder<ResoureAccountTokenParameters.Builder> {

        private String mHomeAccountId;
        private String mAadDeviceId;
        private String mLoginHint;
        private AuthenticationCallback mCallback;

        public ResoureAccountTokenParameters.Builder withHomeAccountId(@NonNull final String homeAccountId) {
            mHomeAccountId = homeAccountId;
            return self();
        }

        public ResoureAccountTokenParameters.Builder withAadDeviceId(final String aadDeviceId) {
            mAadDeviceId = aadDeviceId;
            return self();
        }

        public ResoureAccountTokenParameters.Builder withLoginHint(final String loginHint) {
            mLoginHint = loginHint;
            return self();
        }

        public ResoureAccountTokenParameters.Builder withCallback(
                final AuthenticationCallback authenticationCallback) {
            mCallback = authenticationCallback;
            return self();
        }

        @Override
        public ResoureAccountTokenParameters.Builder self() {
            return this;
        }

        public ResoureAccountTokenParameters build() {
            return new ResoureAccountTokenParameters(this);
        }
    }

}
