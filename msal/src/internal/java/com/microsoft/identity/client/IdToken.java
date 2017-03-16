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

import android.util.Base64;

import org.json.JSONException;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * MSAL internal class that represents the id token.
 */
final class IdToken {
    private final String mIssuer;
    private final String mObjectId;
    private final String mSubject;
    private final String mTenantId;
    private final String mVersion;
    private final String mPreferredName;
    private final String mName;
    private final String mHomeObjectId;

    /**
     * Constructor to create a new {@link IdToken}. Will parse the raw id token.
     * @param rawIdToken The raw Id token used to create the {@link IdToken}.
     */
    IdToken(final String rawIdToken) throws MsalClientException {
        if (MsalUtils.isEmpty(rawIdToken)) {
            throw new IllegalArgumentException("null or empty raw idtoken");
        }

        // set all the instance variables.
        final Map<String, String> idTokenItems = parseJWT(rawIdToken);
        if (idTokenItems == null || idTokenItems.isEmpty()) {
            throw new MsalClientException(MsalError.INVALID_JWT, "Empty Id token returned from server.");
        }

        mIssuer = idTokenItems.get(IdTokenClaim.ISSUER);
        mObjectId = idTokenItems.get(IdTokenClaim.OBJECT_ID);
        mSubject = idTokenItems.get(IdTokenClaim.SUBJECT);
        mTenantId = idTokenItems.get(IdTokenClaim.TENANT_ID);
        mVersion = idTokenItems.get(IdTokenClaim.VERSION);
        mPreferredName = idTokenItems.get(IdTokenClaim.PREFERRED_USERNAME);
        mName = idTokenItems.get(IdTokenClaim.NAME);
        mHomeObjectId = idTokenItems.get(IdTokenClaim.HOME_OBJECT_ID);
    }

    String getIssuer() {
        return mIssuer;
    }

    String getObjectId() {
        return mObjectId;
    }

    String getSubject() {
        return mSubject;
    }

    String getTenantId() {
        return mTenantId;
    }

    String getVersion() {
        return mVersion;
    }

    String getPreferredName() {
        return mPreferredName;
    }

    String getName() {
        return mName;
    }

    String getHomeObjectId() {
        return mHomeObjectId;
    }

    private Map<String, String> parseJWT(final String idToken) throws MsalClientException {
        final String idTokenBody = extractJWTBody(idToken);
        final byte[] data = Base64.decode(idTokenBody, Base64.URL_SAFE);

        try {
            final String decodedBody = new String(data, Charset.forName(MsalUtils.ENCODING_UTF8));
            return MsalUtils.extractJsonObjectIntoMap(decodedBody);
        } catch (final JSONException e) {
            throw new MsalClientException(MsalError.INVALID_JWT, "Failed to extract Json object " + e.getMessage(), e);
        }
    }

    private String extractJWTBody(final String idToken) throws MsalClientException {
        final int firstDot = idToken.indexOf(".");
        final int secondDot = idToken.indexOf(".", firstDot + 1);
        final int invalidDot = idToken.indexOf(".", secondDot + 1);

        if (invalidDot == -1 && firstDot > 0 && secondDot > 0) {
            return idToken.substring(firstDot + 1, secondDot);
        } else {
            throw new MsalClientException(MsalError.INVALID_JWT, "Failed to parse id token.", null);
        }
    }

    private static class IdTokenClaim {
        static final String ISSUER = "iss";
        static final String OBJECT_ID = "oid";
        static final String SUBJECT = "sub";
        static final String TENANT_ID = "tid";
        static final String VERSION = "ver";
        static final String PREFERRED_USERNAME = "preferred_username";
        static final String NAME = "name";
        static final String HOME_OBJECT_ID = "home_oid";
    }
}