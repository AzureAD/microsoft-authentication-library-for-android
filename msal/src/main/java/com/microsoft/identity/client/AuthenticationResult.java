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
import java.util.Set;

/**
 * Created by weij on 8/2/2016.
 */
public final class AuthenticationResult {

    private String mToken;
    private Date mExpiresOn;
    private String mTenantId;
    private User mUser;
    private String mIdToken;
    private String[] mScope;

    /**
     * Constructor to create {@link AuthenticationResult} with {@link TokenResponse}.
     * @param tokenResponse
     */
    AuthenticationResult(final TokenResponse tokenResponse) throws AuthenticationException {
        // If both access token and id token is returned, mToken is set with access token.
        // If access token is not returned but id token is returned, mToken is set with id token.
        if (!MSALUtils.isEmpty(tokenResponse.getAccessToken())) {
            mToken = tokenResponse.getAccessToken();
            mExpiresOn = tokenResponse.getExpiresOn();
        } else if (!MSALUtils.isEmpty(tokenResponse.getRawIdToken())) {
            mToken = tokenResponse.getRawIdToken();
            mExpiresOn = tokenResponse.getIdTokenExpiresOn();
        }

        if (!MSALUtils.isEmpty(tokenResponse.getRawIdToken())) {
            mIdToken = tokenResponse.getRawIdToken();
            final IdToken idToken = new IdToken(tokenResponse.getRawIdToken());
            mTenantId = idToken.getTenantId();
            mUser = new User(idToken);
        }

        final Set<String> returnedScopesInSet = MSALUtils.getScopesAsSet(tokenResponse.getScope());
        mScope = returnedScopesInSet.toArray(new String[returnedScopesInSet.size()]);
    }

    /**
     * Constructor to create {@link AuthenticationResult} with {@link TokenCacheItem}.
     * @param tokenCacheItem
     */
    AuthenticationResult(final TokenCacheItem tokenCacheItem) { }

    /**
     * @return The token, could be access token or id token.
     */
    public String getToken() {
        return mToken;
    }

    /**
     * @return The time that the access token returned in the Token property ceases to be valid.
     * This value is calculated based on current UTC time measured locally and the value expiresIn returned from the
     * service.
     */
    public Date getExpiresOn() {
        return mExpiresOn;
    }

    /**
     * @return An identifier for the tenant that the token was acquired from. Could be null if tenant information is not
     * returned by the service.
     */
    public String getTenantId() {
        return mTenantId;
    }

    /**
     * @return {@link User} that identifies the user information. Some elements in {@link User} could be null if not
     * returned by the service.
     */
    public User getUser() {
        return mUser;
    }

    /**
     * @return The raw Id token returned from service. Could be null if it's not returned.
     */
    // TODO: remove should we return? if Id token is signed, we shouldn't return it.
    public String getIdToken() {
        return mIdToken;
    }

    /**
     * @return The scope values returned from the service.
     */
    public String[] getScope() {
        return mScope;
    }
}