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

import com.microsoft.identity.client.internal.authscheme.AuthenticationSchemeParameters;
import com.microsoft.identity.common.internal.authscheme.IPoPAuthenticationSchemeParams;
import com.microsoft.identity.common.internal.authscheme.PopAuthenticationSchemeInternal;

import java.net.URL;
import java.util.UUID;

/**
 * Required Params for a Proof-of-Possession protected request.
 */
public class ProofOfPossessionParameters
        extends AuthenticationSchemeParameters
        implements IPoPAuthenticationSchemeParams {

    private final String mHttpMethod;
    private final URL mUrl;
    private final String mNonce;

    /**
     * Constructs a new ProofOfPossessionParameters.
     *
     * @param method The HTTP method of the resource request.
     * @param url    The URL of PoP token recipient (resource).
     */
    public ProofOfPossessionParameters(@NonNull final HttpMethod method,
                                       @NonNull final URL url) {
        super(PopAuthenticationSchemeInternal.SCHEME_POP);
        mHttpMethod = method.name();
        mUrl = url;
        mNonce = UUID.randomUUID().toString();
    }

    /**
     * Gets the {@link HttpMethod}.
     *
     * @return The HttpMethod to get.
     */
    @Override
    public String getHttpMethod() {
        return mHttpMethod;
    }

    /**
     * Gets the url.
     *
     * @return The url to get.
     */
    @Override
    public URL getUrl() {
        return mUrl;
    }

    /**
     * Gets the nonce.
     *
     * @return The nonce to get.
     */
    @Override
    public String getNonce() {
        return mNonce;
    }
}
