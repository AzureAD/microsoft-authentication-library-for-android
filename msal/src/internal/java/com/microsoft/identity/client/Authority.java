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

import java.net.MalformedURLException;
import java.net.URL;

/**
 * MSAL internal representation for the authority.
 */
final class Authority {
    private static final String TAG = Authority.class.getSimpleName();

    private static final String HTTPS_PROTOCOL = "https";
    private final URL mAuthorityUrl;

    /**
     * Constructor for the {@link Authority}.
     * @param authorityUrl The string representation for the authority url.
     */
    Authority(final String authorityUrl, final boolean validateAuthority) {

        try {
            mAuthorityUrl = new URL(authorityUrl.endsWith("/") ? authorityUrl : authorityUrl + "/");
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException("malformed authority url.");
        }

        if (!HTTPS_PROTOCOL.equalsIgnoreCase(mAuthorityUrl.getProtocol())) {
            throw new IllegalArgumentException("Invalid protocol for the authority url.");
        }

        if (validateAuthority) {
            // TODO: perform authority validation. Authority validation needs network call, consider using async task
            // to perform the valiation.
        }
    }

    /**
     * @return The authority url.
     */
    URL getAuthorityUrl() {
        return mAuthorityUrl;
    }
}