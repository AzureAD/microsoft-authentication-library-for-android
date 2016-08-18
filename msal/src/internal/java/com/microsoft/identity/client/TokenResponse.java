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

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal class for the the returned JSON response from token endpoint.
 */
final class TokenResponse {
    private String mAccessToken;
    private String mRawIdToken;
    private String mRefreshToken;
    private Date mExpiresOn;
    private Date mExtendedExpiresOn;
    private Date mIdTokenExpiresOn;
    private String mScope;
    private String mTokenType;
    private String mFoCI;
    // TODO: remove the client info for now.
//    private ClientInfo mClientInfo;
    private String mError;
    private String mErrorDescription;
    private String[] mErrorCodes;
    private final Map<String, String> mAdditionalData = new HashMap<>();

    public TokenResponse(final String accessToken, final String rawIdToken, final String refreshToken,
                         final Date expiresOn, final Date idTokenExpiresOn, final Date extendedExpiresOn,
                         final String scope, final String tokenType, final String foCI) {
        mAccessToken = accessToken;
        mRawIdToken = rawIdToken;
        mRefreshToken = refreshToken;
        mExpiresOn = expiresOn;
        mIdTokenExpiresOn = idTokenExpiresOn;
        mExtendedExpiresOn = extendedExpiresOn;
        mScope = scope;
        mTokenType = tokenType;
        mFoCI = foCI;
        mError = null;
        mErrorDescription = null;
    }

    public TokenResponse(final String error, final String errorDescription, String[] errorCodes) {
        mError = error;
        mErrorDescription = errorDescription;
        mErrorCodes = errorCodes;

        mAccessToken = null;
        mRawIdToken = null;
        mExpiresOn = null;
        mExtendedExpiresOn = null;
        mScope = null;
        mTokenType = null;
        mFoCI = null;
    }

    public void setAccessToken(final String accessToken) {
        mAccessToken = accessToken;
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public void setRawIdToken(final String rawIdToken) {
        mRawIdToken = rawIdToken;
    }

    public String getRawIdToken() {
        return mRawIdToken;
    }

    public void setRefreshToken(final String refreshToken) {
        mRefreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return mRefreshToken;
    }

    public void setExpiresOn(final Date expiresOn) {
        mExpiresOn = expiresOn;
    }

    public Date getExpiresOn() {
        return mExpiresOn;
    }

    public void setIdTokenExpiresOn(final Date idTokenExpiresOn) {
        mIdTokenExpiresOn = idTokenExpiresOn;
    }

    public Date getIdTokenExpiresOn() {
        return mIdTokenExpiresOn;
    }

    public void setExtendedExpiresOn(final Date extendedExpiresOn) {
        mExtendedExpiresOn = extendedExpiresOn;
    }

    public Date getExtendedExpiresOn() {
        return mExtendedExpiresOn;
    }

    public void setScope(final String scope) {
        mScope = scope;
    }

    public String getScope() {
        return mScope;
    }

    public void setTokenType(final String tokenType) {
        mTokenType = tokenType;
    }

    public String getmTokenType() {
        return mTokenType;
    }

    public void setFamilyClientId(final String foCI) {
        mFoCI = foCI;
    }

    public String getFamilyClientId() {
        return mFoCI;
    }

    public void setError(final String error) {
        mError = error;
    }

    public String getError() {
        return mError;
    }

    public void setErrorDescription(final String errorDescription) {
        mErrorDescription = errorDescription;
    }

    public void setAdditionalData(final Map<String, String> additionalData) {
        mAdditionalData.putAll(additionalData);
    }

    public Map<String, String> getAdditionalData() {
        return mAdditionalData;
    }

    public String getErrorDescription() {
        return mErrorDescription;
    }

    static TokenResponse createTokenResponseWithError(final Map<String, String> responseItems) throws JSONException {
        final String error = responseItems.get(TokenResponseClaim.ERROR);
        final String errorDescription = responseItems.get(TokenResponseClaim.ERROR_DESCRIPTION);

        final JSONArray errorCodesJsonArray = new JSONArray(responseItems.get(TokenResponseClaim.ERROR_CODES));
        final List<String> errorCodesList = new ArrayList<>();
        for (int i = 0; i < errorCodesJsonArray.length(); i++) {
            final String errorCode = errorCodesJsonArray.getString(i);
            errorCodesList.add(errorCode);
        }

        return new TokenResponse(error, errorDescription, errorCodesList.toArray(new String[errorCodesList.size()]));
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

        final String familyId = responseItems.get(TokenResponseClaim.FAMILY_ID);
        additionalData.remove(TokenResponseClaim.FAMILY_ID);

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
                idTokenExpiresOn, extendedExpiresOn, scope, tokenType, familyId);
        tokenResponse.setAdditionalData(additionalData);

        return tokenResponse;
    }
}