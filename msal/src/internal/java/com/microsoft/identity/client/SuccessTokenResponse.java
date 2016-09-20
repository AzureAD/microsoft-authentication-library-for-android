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
 * MSAL internal class that stands for the success token response. Will read the token response, if access token is returned,
 * token will be set with access token and expiresOn, otherwise it will be set as id token and id token expires on.
 */
final class SuccessTokenResponse {
    private String mToken;
    private Date mExpiresOn;
    private String mRefreshToken;
    private String mIdToken;
    private Date mExtendedExpiresOn;

    private String mScope;
    private String mTokenType;
    private String mFoci;
    private User mUser;
    private String mTenantId;

    SuccessTokenResponse(final TokenResponse response) throws AuthenticationException {
        if (!MSALUtils.isEmpty(response.getRawIdToken())) {
            mIdToken = response.getRawIdToken();
            final IdToken idToken = new IdToken(mIdToken);
            mTenantId = idToken.getTenantId();
            mUser = new User(idToken);
        }

        // If both access token and id token is returned, mToken is set with access token.
        // If access token is not returned but id token is returned, mToken is set with id token.
        if (!MSALUtils.isEmpty(response.getAccessToken())) {
            mToken = response.getAccessToken();
            mExpiresOn = response.getExpiresOn();
        } else if (!MSALUtils.isEmpty(response.getRawIdToken())) {
            mToken = response.getRawIdToken();
            mExpiresOn = response.getIdTokenExpiresOn();
        }

        mScope = response.getScope();
        mFoci = response.getFamilyClientId();
        mExtendedExpiresOn = response.getExtendedExpiresOn();
        mRefreshToken = response.getRefreshToken();
        mTokenType = response.getTokenType();
    }

    String getToken() {
        return mToken;
    }

    Date getExpiresOn() {
        return mExpiresOn;
    }

    String getRefreshToken() {
        return mRefreshToken;
    }

    User getUser() {
        return mUser;
    }

    String getRawIdToken() {
        return mIdToken;
    }

    String getFamilyClientId() {
        return mFoci;
    }

    String getTokenType() {
        return mTokenType;
    }

    String getScope() {
        return mScope;
    }

    String getTenantId() {
        return mTenantId;
    }

    Date getExtendedExpiresOn() {
        return mExtendedExpiresOn;
    }
}
