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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.java.authscheme.IPoPAuthenticationSchemeParams;
import com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeInternal;

import java.net.URL;

public class PoPAuthenticationScheme extends AuthenticationScheme
        implements IPoPAuthenticationSchemeParams {

    private final URL mUrl;
    private final HttpMethod mHttpMethod;
    private final String mNonce;
    private final String mClientClaims;

    private PoPAuthenticationScheme(
            @NonNull final HttpMethod method,
            @NonNull final URL url,
            @Nullable final String nonce,
            @Nullable final String clientClaims) {
        super(PopAuthenticationSchemeInternal.SCHEME_POP);
        mHttpMethod = method;
        mUrl = url;
        mNonce = nonce;
        mClientClaims = clientClaims;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    @Nullable
    public String getHttpMethod() {
        String httpMethod = null;

        if (null != mHttpMethod) { // HTTP method is optional
            httpMethod = mHttpMethod.name();
        }

        return httpMethod;
    }

    @Override
    public URL getUrl() {
        return mUrl;
    }

    @Override
    public String getClientClaims() {
        return mClientClaims;
    }

    @Override
    public String getNonce() {
        return mNonce;
    }

    public static class Builder {

        private URL mUrl;
        private HttpMethod mHttpMethod;
        private String mNonce;
        private String mClientClaims;

        private Builder() {
            // Intentionally blank
        }

        public Builder withUrl(@NonNull final URL url) {
            mUrl = url;
            return this;
        }

        public Builder withHttpMethod(@NonNull final HttpMethod method) {
            mHttpMethod = method;
            return this;
        }

        public Builder withNonce(@Nullable final String nonce) {
            mNonce = nonce;
            return this;
        }

        /**
         * Sets the client_claims to be embedded in the resulting SHR.
         * <p>
         * Important: Use of this API requires setting the minimum_required_broker_protocol_version to
         * "6.0" or higher.
         *
         * @param clientClaims A string of arbitrary data to be signed into the resulting Signed
         *                     HTTP Request (SHR).
         * @return This Builder.
         */
        public Builder withClientClaims(@Nullable final String clientClaims) {
            mClientClaims = clientClaims;
            return this;
        }

        public PoPAuthenticationScheme build() {
            String errMsg = "PoP authentication scheme param must not be null: ";

            if (null == mUrl) {
                throw new IllegalArgumentException(errMsg + "URL");
            }

            return new PoPAuthenticationScheme(mHttpMethod, mUrl, mNonce, mClientClaims);
        }
    }
}
