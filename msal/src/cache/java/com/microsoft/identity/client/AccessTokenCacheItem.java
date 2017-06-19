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

import com.google.gson.annotations.SerializedName;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;

/**
 * MSAL internal class for representing the access token cache item.
 */
final class AccessTokenCacheItem extends BaseTokenCacheItem {

    private static final int DEFAULT_EXPIRATION_BUFFER = 300;

    @SerializedName("authority")
    final String mAuthority;

    @SerializedName("access_token")
    private String mAccessToken;

    @SerializedName("expires_on")
    private long mExpiresOn;

    @SerializedName("scope")
    final String mScope;

    @SerializedName("token_type")
    final String mTokenType;

    @SerializedName("id_token")
    final String mRawIdToken;

    /**
     * No args constructor for use ill serialization for Gson to prevent usage of sun.misc.Unsafe
     */
    @SuppressWarnings("unused")
    AccessTokenCacheItem() throws MsalClientException {
      this(null, null, null);
    }

    /**
     * Constructor for creating the {@link AccessTokenCacheItem}.
     */
    AccessTokenCacheItem(final String authority, final String clientId, final TokenResponse response)
            throws MsalClientException {
        super(clientId, response.getRawClientInfo());

        mAuthority = authority;
        mAccessToken = response.getAccessToken();
        mExpiresOn = response.getExpiresOn().getTime();
        mScope = response.getScope();
        mTokenType = response.getTokenType();
        mRawIdToken = response.getRawIdToken();

        final IdToken idToken = new IdToken(mRawIdToken);
        mUser = User.create(idToken, new ClientInfo(mRawClientInfo));
    }

    @Override
    AccessTokenCacheKey extractTokenCacheKey() {
        return AccessTokenCacheKey.createTokenCacheKey(mAuthority, mClientId, MsalUtils.getScopesAsSet(mScope), mUser);
    }

    /**
     * @return The authority for the request.
     */
    String getAuthority() {
        return mAuthority;
    }

    /**
     * @return The access token returned in the token respone.
     */
    String getAccessToken() {
        return mAccessToken;
    }

    /**
     * @return The access token expires on.
     */
    Date getExpiresOn() {
        return new Date(mExpiresOn);
    }

    /**
     * @return Scopes in the format of set.
     */
    Set<String> getScope() {
        return MsalUtils.getScopesAsSet(mScope);
    }

    /**
     * @return The token type, i.e Bearer.
     */
    String getTokenType() {
        return mTokenType;
    }

    /**
     * @return The raw id token.
     */
    String getRawIdToken() {
        return mRawIdToken;
    }

    String getRawClientInfo() {
        return mRawClientInfo;
    }

    /**
     * @return True if the token cache item is already expired, false otherwise.
     */
    boolean isExpired() {
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, DEFAULT_EXPIRATION_BUFFER);
        final Date validity = calendar.getTime();

        final Date expiresOn = getExpiresOn();
        return expiresOn != null && expiresOn.before(validity);
    }

    IdToken getIdToken() throws MsalClientException {
        return new IdToken(mRawIdToken);
    }
}
