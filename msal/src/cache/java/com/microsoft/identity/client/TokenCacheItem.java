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

import java.util.Date;

/**
 * MSAL internal class for representing the access token cache item.
 */
final class TokenCacheItem extends BaseTokenCacheItem {

    private String mToken;
    private Date mExpiresOn;

    /**
     * Constructor for creating the {@link TokenCacheItem}.
     */
    TokenCacheItem(final String authority, final String clientId, final TokenResponse response) throws AuthenticationException {
        super(authority, clientId, response);

        if (!MSALUtils.isEmpty(response.getAccessToken())) {
            mToken = response.getAccessToken();
            mExpiresOn = response.getExpiresOn();
        } else if (!MSALUtils.isEmpty(response.getRawIdToken())) {
            mToken = response.getRawIdToken();
            mExpiresOn = response.getIdTokenExpiresOn();
        }
    }

    /**
     * @return The token. Could either be access token or id token.
     */
    String getToken() {
        return mToken;
    }

    /**
     * @return The expires on. Could either be access token expires on or id token expires on.
     */
    Date getExpiresOn() {
        return mExpiresOn;
    }
}
