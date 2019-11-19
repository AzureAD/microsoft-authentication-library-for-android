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

import com.microsoft.identity.common.internal.authscheme.IPoPAuthenticationSchemeParams;
import com.microsoft.identity.common.internal.authscheme.PopAuthenticationSchemeInternal;

import java.net.URL;
import java.util.UUID;

import static com.microsoft.identity.client.internal.MsalUtils.validateNonNullArgument;

public class PopAuthenticationScheme
        extends AuthenticationScheme
        implements IPoPAuthenticationSchemeParams {

    private static final String ARG_HTTP_METHOD = "httpMethod";
    private static final String ARG_URL = "url";
    private static final String ARG_NONCE = "nonce";

    private String mHttpMethod;
    private URL mUrl;
    private String mNonce;

    /**
     * Constructs a new PopAuthenticationScheme.
     */
    public PopAuthenticationScheme(@NonNull final HttpMethod httpMethod,
                                   @NonNull final URL url) {
        super(PopAuthenticationSchemeInternal.SCHEME_POP);

        // Validate args
        validateNonNullArgument(httpMethod, ARG_HTTP_METHOD);
        validateNonNullArgument(url, ARG_URL);

        mHttpMethod = httpMethod.name();
        mUrl = url;
        mNonce = UUID.randomUUID().toString();
    }

    private PopAuthenticationScheme(@NonNull final HttpMethod httpMethod,
                                    @NonNull final URL url,
                                    @NonNull final String nonce) {
        super(PopAuthenticationSchemeInternal.SCHEME_POP);
        mHttpMethod = httpMethod.name();
        mUrl = url;
        mNonce = nonce;
    }

    @Override
    public String getHttpMethod() {
        return mHttpMethod;
    }

    @Override
    public URL getUrl() {
        return mUrl;
    }

    @Override
    public String getNonce() {
        return mNonce;
    }

    public static class Builder {

        private HttpMethod mHttpMethod;
        private URL mUrl;
        private String mNonce;

        public Builder(@NonNull final HttpMethod httpMethod,
                       @NonNull final URL url) {
            // Validate args
            validateNonNullArgument(httpMethod, ARG_HTTP_METHOD);
            validateNonNullArgument(url, ARG_URL);

            mHttpMethod = httpMethod;
            mUrl = url;
        }

        @NonNull
        public Builder withNonce(@NonNull final String nonce) {
            validateNonNullArgument(nonce, ARG_NONCE);
            mNonce = nonce;
            return this;
        }

        @NonNull
        public PopAuthenticationScheme build() {
            return new PopAuthenticationScheme(
                    mHttpMethod,
                    mUrl,
                    mNonce
            );
        }
    }
}
