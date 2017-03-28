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

import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link TokenResponse}.
 */
@RunWith(AndroidJUnit4.class)
public final class TokenResponseTest {
    static final String ACCESS_TOKEN = "some access token";
    static final String REFRESH_TOKEN = "some refresh token";
    static final String TOKEN_TYPE = "some token type";
    static final String SCOPE = "scope1 scope2";
    static final String EXTRA_DATA_1 = "extraData1";
    static final String EXTRA_DATA_2 = "extraData2";
    static final String ERROR = "error_code";
    static final String ERROR_DESCRIPTION = "some error description";
    static final String ERROR_CODES = "[1234, 3456]";

    @Test
    public void testTokenResponseWithSuccessResponse() {
        final Map<String, String> successResponseItem = new HashMap<>();
        successResponseItem.put(OauthConstants.TokenResponseClaim.ACCESS_TOKEN, ACCESS_TOKEN);
        successResponseItem.put(OauthConstants.TokenResponseClaim.REFRESH_TOKEN, REFRESH_TOKEN);
        successResponseItem.put(OauthConstants.TokenResponseClaim.TOKEN_TYPE, TOKEN_TYPE);
        successResponseItem.put(OauthConstants.TokenResponseClaim.SCOPE, SCOPE);
        successResponseItem.put(EXTRA_DATA_1, EXTRA_DATA_1);
        successResponseItem.put(EXTRA_DATA_2, EXTRA_DATA_2);

        final TokenResponse successResponse = TokenResponse.createSuccessTokenResponse(successResponseItem);
        Assert.assertTrue(ACCESS_TOKEN.equals(successResponse.getAccessToken()));
        Assert.assertTrue(REFRESH_TOKEN.equals(successResponse.getRefreshToken()));
        Assert.assertTrue(TOKEN_TYPE.equals(successResponse.getTokenType()));
        Assert.assertTrue(SCOPE.equals(successResponse.getScope()));

        final Map<String, String> additionalData = successResponse.getAdditionalData();
        Assert.assertFalse(additionalData == null || additionalData.isEmpty());
        Assert.assertTrue(additionalData.size() == 2);
        Assert.assertTrue(EXTRA_DATA_1.equals(additionalData.get(EXTRA_DATA_1)));
        Assert.assertTrue(EXTRA_DATA_2.equals(additionalData.get(EXTRA_DATA_2)));
    }

    @Test
    public void testTokenResponseWithFailureResponse() throws JSONException {
        final Map<String, String> failureResponseItem = new HashMap<>();
        failureResponseItem.put(OauthConstants.BaseOauth2ResponseClaim.ERROR, ERROR);
        failureResponseItem.put(OauthConstants.BaseOauth2ResponseClaim.ERROR_DESCRIPTION, ERROR_DESCRIPTION);
        failureResponseItem.put(OauthConstants.BaseOauth2ResponseClaim.ERROR_CODES, ERROR_CODES);
        failureResponseItem.put(EXTRA_DATA_1, EXTRA_DATA_1);
        failureResponseItem.put(EXTRA_DATA_1, EXTRA_DATA_2);

        final TokenResponse failureResponse = new TokenResponse(BaseOauth2Response.createErrorResponse(failureResponseItem,
                HttpURLConnection.HTTP_BAD_REQUEST), null);
        Assert.assertTrue(ERROR.equals(failureResponse.getError()));
        Assert.assertTrue(ERROR_DESCRIPTION.equals(failureResponse.getErrorDescription()));

    }

    @Test
    public void testConstructorForSuccessTokenResponse() {
        final String rawIdToken = "raw idtoken";
        final Date expiresOn = new Date();
        final TokenResponse tokenResponse = new TokenResponse(ACCESS_TOKEN, rawIdToken, REFRESH_TOKEN, expiresOn,
                expiresOn, expiresOn, SCOPE, TOKEN_TYPE, null);

        Assert.assertTrue(ACCESS_TOKEN.equals(tokenResponse.getAccessToken()));
        Assert.assertTrue(rawIdToken.equals(tokenResponse.getRawIdToken()));
        Assert.assertTrue(REFRESH_TOKEN.equals(tokenResponse.getRefreshToken()));
        Assert.assertTrue(expiresOn.equals(tokenResponse.getExpiresOn()));
        Assert.assertTrue(expiresOn.equals(tokenResponse.getIdTokenExpiresOn()));
        Assert.assertTrue(expiresOn.equals(tokenResponse.getExtendedExpiresOn()));
        Assert.assertTrue(TOKEN_TYPE.equals(tokenResponse.getTokenType()));
        Assert.assertTrue(SCOPE.equals(tokenResponse.getScope()));
    }

    @Test
    public void testConstructorForFailureResponse() {
        final String claims = "some/claim";
        final TokenResponse response = new TokenResponse(ERROR, ERROR_DESCRIPTION, HttpURLConnection.HTTP_BAD_REQUEST, claims);

        Assert.assertTrue(ERROR.equals(response.getError()));
        Assert.assertTrue(ERROR_DESCRIPTION.equals(response.getErrorDescription()));
        Assert.assertTrue(response.getHttpStatusCode() == HttpURLConnection.HTTP_BAD_REQUEST);
        Assert.assertTrue(response.getClaims().equals(claims));
    }
}
