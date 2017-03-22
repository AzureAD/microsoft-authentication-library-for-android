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

import com.microsoft.identity.client.OauthConstants.TokenResponseClaim;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Internal class for the the returned JSON response from token endpoint.
 */
final class TokenResponse extends BaseOauth2Response {
    private final String mAccessToken;
    private final String mRawIdToken;
    private final String mRefreshToken;
    private final Date mExpiresOn;
    private final Date mExtendedExpiresOn;
    private final Date mIdTokenExpiresOn;
    private final String mScope;
    private final String mTokenType;
    private final Map<String, String> mAdditionalData = new HashMap<>();

    private final String mClaims;

    /**
     * Create token response with token when token is returned.
     */
    public TokenResponse(final String accessToken, final String rawIdToken, final String refreshToken,
                         final Date expiresOn, final Date idTokenExpiresOn, final Date extendedExpiresOn,
                         final String scope, final String tokenType) {
        // success response: error, errorDescription and errorCodes are all null
        super(null, null, BaseOauth2Response.DEFAULT_STATUS_CODE);
        mClaims = null;
        mAccessToken = accessToken;
        mRawIdToken = rawIdToken;
        mRefreshToken = refreshToken;
        mExpiresOn = expiresOn;
        mIdTokenExpiresOn = idTokenExpiresOn;
        mExtendedExpiresOn = extendedExpiresOn;
        mScope = scope;
        mTokenType = tokenType;
    }

    /**
     * Creates token response with error returned in the server JSON response.
     */
    public TokenResponse(final String error, final String errorDescription, final int statusCode, final String claims) {
        super(error, errorDescription, statusCode);
        mClaims = claims;

        mAccessToken = null;
        mRefreshToken = null;
        mRawIdToken = null;
        mExpiresOn = null;
        mIdTokenExpiresOn = null;
        mExtendedExpiresOn = null;
        mScope = null;
        mTokenType = null;
    }

    TokenResponse(final BaseOauth2Response baseOauth2Response, final String claims) {
        this(baseOauth2Response.getError(), baseOauth2Response.getErrorDescription(), baseOauth2Response.getHttpStatusCode(), claims);
    }

    /**
     * @return The access token.
     */
    public String getAccessToken() {
        return mAccessToken;
    }

    /**
     * @return The raw id token.
     */
    public String getRawIdToken() {
        return mRawIdToken;
    }

    /**
     * @return The refresh token.
     */
    public String getRefreshToken() {
        return mRefreshToken;
    }

    /**
     * @return Expires for the access token.
     */
    public Date getExpiresOn() {
        return mExpiresOn;
    }

    /**
     * @return Expires on for the id token.
     */
    public Date getIdTokenExpiresOn() {
        return mIdTokenExpiresOn;
    }

    /**
     * @return Extended expires on for the access token.
     */
    public Date getExtendedExpiresOn() {
        return mExtendedExpiresOn;
    }

    /**
     * @return Scopes returned in the server response.
     */
    public String getScope() {
        return mScope;
    }

    /**
     * @return The token type.
     */
    public String getTokenType() {
        return mTokenType;
    }

    public String getClaims() {
        return mClaims;
    }

    /**
     * Set additional data returned in the server response.
     * @param additionalData The additional data in the JSON response.
     */
    public void setAdditionalData(final Map<String, String> additionalData) {
        mAdditionalData.putAll(additionalData);
    }

    /**
     * @return The additional data returned from server.
     */
    public Map<String, String> getAdditionalData() {
        return mAdditionalData;
    }

    static TokenResponse createSuccessTokenResponse(final Map<String, String> responseItems) {
        final Map<String, String> additionalData = new HashMap<>(responseItems);
        final String tokenType = responseItems.get(TokenResponseClaim.TOKEN_TYPE);
        additionalData.remove(TokenResponseClaim.TOKEN_TYPE);

        final String accessToken = responseItems.get(TokenResponseClaim.ACCESS_TOKEN);
        additionalData.remove(TokenResponseClaim.ACCESS_TOKEN);

        final String refreshToken = responseItems.get(TokenResponseClaim.REFRESH_TOKEN);
        additionalData.remove(TokenResponseClaim.REFRESH_TOKEN);

        final String scope = responseItems.get(TokenResponseClaim.SCOPE);
        additionalData.remove(TokenResponseClaim.SCOPE);

        final String idToken = responseItems.get(TokenResponseClaim.ID_TOKEN);
        additionalData.remove(TokenResponseClaim.ID_TOKEN);

        final Date expiresOn = MSALUtils.calculateExpiresOn(responseItems.get(TokenResponseClaim.EXPIRES_IN));
        additionalData.remove(TokenResponseClaim.EXPIRES_IN);

        final Date idTokenExpiresOn = MSALUtils.calculateExpiresOn(responseItems.get(
                TokenResponseClaim.ID_TOKEN_EXPIRES_IN));
        additionalData.remove(TokenResponseClaim.ID_TOKEN_EXPIRES_IN);

        final Date extendedExpiresOn = MSALUtils.calculateExpiresOn(responseItems.get(
                TokenResponseClaim.EXTENDED_EXPIRES_IN));
        additionalData.remove(TokenResponseClaim.EXTENDED_EXPIRES_IN);

        final TokenResponse tokenResponse = new TokenResponse(accessToken, idToken, refreshToken, expiresOn,
                idTokenExpiresOn, extendedExpiresOn, scope, tokenType);
        tokenResponse.setAdditionalData(additionalData);

        return tokenResponse;
    }

    static TokenResponse createFailureTokenResponse(final Map<String, String> responseItems, int statusCode) {
        final String claims = responseItems.get(OauthConstants.BaseOauth2ResponseClaim.CLAIMS);

        return new TokenResponse(BaseOauth2Response.createErrorResponse(responseItems, statusCode), claims);
    }
}