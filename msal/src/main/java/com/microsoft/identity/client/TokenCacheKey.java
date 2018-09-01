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

import java.util.Locale;

/**
 * Object used to group the all the conditions to lookup into token cache. Base token cache key class.
 */
abstract class TokenCacheKey<T extends BaseTokenCacheItem> {
    static final String TOKEN_CACHE_KEY_DELIMITER = "$";
    final String mClientId;
    final String mUserIdentifier;

    TokenCacheKey(final String clientId, final String uid, final String utid) {
        // All the tokens issued by AAD is cross tenant, ADFS 2016 should work the same as AAD, and client id.
        if (MsalUtils.isEmpty(clientId)) {
            throw new IllegalArgumentException("clientId");
        }

        mClientId = clientId.toLowerCase(Locale.US);
        // if uid or utid is empty, the unique identifier will be "."
        mUserIdentifier = MsalUtils.getUniqueUserIdentifier(uid, utid);
    }

    abstract boolean matches(T tokenCacheItem);
}
