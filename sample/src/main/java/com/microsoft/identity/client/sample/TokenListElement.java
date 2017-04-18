//   Copyright (c) Microsoft Corporation.
//   All rights reserved.
//
//   This code is licensed under the MIT License.
//
//   Permission is hereby granted, free of charge, to any person obtaining a copy
//   of this software and associated documentation files(the "Software"), to deal
//   in the Software without restriction, including without limitation the rights
//   to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//   copies of the Software, and to permit persons to whom the Software is
//   furnished to do so, subject to the following conditions :
//
//   The above copyright notice and this permission notice shall be included in
//   all copies or substantial portions of the Software.
//
//   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//   THE SOFTWARE.

package com.microsoft.identity.client.sample;

import java.io.Serializable;

/**
 * Elements for display in {@link CacheFragment}
 */
final class TokenListElement implements Serializable {

    enum ElementType {
        AT("ACCESS TOKEN"),
        RT("REFRESH TOKEN");

        final String mDisplayValue;

        ElementType(final String displayVal) {
            mDisplayValue = displayVal;
        }
    }

    private final ElementType mElementType;
    private final String mClientId;
    private final String mUserIdentifier;
    private final String mDisplayableId;
    private final String mScopes;
    private final String mExpiresOn;
    private final String mHost;

    TokenListElement(final String clientId, final String userIdentifier, final String displayableId,
                     final String scopes, final String expiresOn) {
        mElementType = ElementType.AT;
        mClientId = clientId;
        mUserIdentifier = userIdentifier;
        mDisplayableId = displayableId;
        mScopes = scopes;
        mExpiresOn = expiresOn;
        mHost = null;
    }

    TokenListElement(final String clientId, final String userIdentifier, final String displayableId,
                     final String host) {
        mElementType = ElementType.RT;
        mClientId = clientId;
        mUserIdentifier = userIdentifier;
        mDisplayableId = displayableId;
        mHost = host;
        mScopes = mExpiresOn = null;
    }

    ElementType getElementType() {
        return mElementType;
    }

    String getClientId() {
        return mClientId;
    }

    String getUserIdentifier() {
        return mUserIdentifier;
    }

    String getDisplayableId() {
        return mDisplayableId;
    }

    String getScopes() {
        return mScopes;
    }

    String getExpiresOn() {
        return mExpiresOn;
    }

    String getHost() {
        return mHost;
    }
}
