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

import com.microsoft.identity.common.internal.authscheme.PopAuthenticationSchemeInternal;

import java.net.URL;
import java.util.UUID;

public class PopAuthenticationScheme extends PopAuthenticationSchemeInternal {

    public PopAuthenticationScheme(@NonNull final String httpMethod,
                                   @NonNull final URL url) {
        this(httpMethod, url, UUID.randomUUID().toString());
    }

    private PopAuthenticationScheme(@NonNull final String httpMethod,
                                    @NonNull final URL url,
                                    @NonNull final String nonce) {
        super(httpMethod, url, nonce);
    }

    public static class Builder {

        private String mMethod;
        private URL mUrl;
        private String mNonce;

        public Builder(@NonNull final String method,
                       @NonNull final URL url) {
            mMethod = method;
            mUrl = url;
        }

        @NonNull
        public Builder withNonce(@NonNull final String nonce) {
            mNonce = nonce;
            return this;
        }

        public PopAuthenticationScheme build() {
            return new PopAuthenticationScheme(mMethod, mUrl, mNonce);
        }
    }
}
