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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

/**
 * Util class for instrumentation tests.
 */
public final class AndroidTestUtil {
    private static final String ACCESS_TOKEN_SHARED_PREFERENCE = "com.microsoft.identity.client.token";
    private static final String REFRESH_TOKEN_SHARED_PREFERENCE = "com.microsoft.identity.client.refreshToken";
    static final String DEFAULT_AUTHORITY = "https://login.microsoftonline.com/common";
    static final int TOKEN_EXPIRATION_IN_MINUTES = 60;

    static final String AUDIENCE = "audience-for-testing";
    static final String TENANT_ID = "6fd1f5cd-a94c-4335-889b-6c598e6d8048";
    static final String OBJECT_ID = "53c6acf2-2742-4538-918d-e78257ec8516";
    static final String PREFERRED_USERNAME = "test@test.onmicrosoft.com";
    static final String ISSUER = "https://login.onmicrosoftonline.com/test/v2.0";
    static final String SUBJECT = "testsomesubject";
    static final String VERSION = "2.0";
    static final String NAME = "test";
    static final String HOME_OBJECT_ID = "some.home.objid";
    static final String ACCESS_TOKEN = "access_token";
    static final String REFRESH_TOKEN = "refresh_token";

    /**
     * Private to prevent util class from being initiated.
     */
    private AndroidTestUtil() {
        // Leave as blank intentionally.
    }

    static final String TEST_IDTOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik1uQ19WWmNBVGZNNXBPWWlKSE1iYTlnb0VLWSJ9.eyJhdWQiOiI"
            + "1YTQzNDY5MS1jY2IyLTRmZDEtYjk3Yi1iNjRiY2ZiYzAzZmMiLCJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vMDI4N2Y5NjMtMmQ3M"
            + "i00MzYzLTllM2EtNTcwNWM1YjBmMDMxL3YyLjAiLCJpYXQiOjE0NzE0OTczMTYsIm5iZiI6MTQ3MTQ5NzMxNiwiZXhwIjoxNDcxNTAxMjE2LCJuYW1lIjoiTW"
            + "FtIFVzZXIiLCJvaWQiOiI2NDI5MTVhNC1kMDViLTRhYjctYmZmYi1mYWYyYjcxNzc4ZWYiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJtYW1AbXNkZXZleC5vbm1"
            + "pY3Jvc29mdC5jb20iLCJzdWIiOiJ6X01zenhybUd0eTkyeWRjNlVybm5JVW1QdjhETXBMRVh5VzU3RmJhb3pvIiwidGlkIjoiMDI4N2Y5NjMtMmQ3Mi00MzYz"
            + "LTllM2EtNTcwNWM1YjBmMDMxIiwidmVyIjoiMi4wIn0.WxDB0BvZXSwWJWyMlMunIyfNAP8hi6rRskuQ1D2oSCH_QyaNI2WCcfhIuuw0tp2fs_NBKwDyK1Yt"
            + "U_14gMN7jL-QB85WvTGx_U-Rg4U5GzyLfmZgWCm6Kap5_mOQkzgdKa3Izxnr42QG0gKP1fg-ndwp3IQUtvFTureCnyDanHydGUCX1KEoQe2Rb0uuuL10xLb5"
            + "UyIOCfAj5cRrZSEStJGyuZnq9nB2t6baM4HGdT4S7Q0madLgb5RPTfI3jMfX47ndnrqFBRpTFbDr4HN9tgXzs9d8EpcAtypp9osD2nh3KBmA77NsZsAMYe0R"
            + "wMHoq4dFkuHYTmywUDqWRha_2w";

    static String createIdToken(final String audience, final String issuer, final String name,
                                final String objectId, final String preferredName,
                                final String subject, final String tenantId, final String version,
                                final String homeObjectId) {
        final String idTokenHeader = "{\"typ\":\"JWT\",\"alg\":\"RS256\"}";
        final String claims = "{\"aud\":\"" + audience + "\",\"iss\":\"" + issuer
                + "\",\"ver\":\"" + version + "\",\"tid\":\"" + tenantId + "\",\"oid\":\"" + objectId
                + "\",\"preferred_username\":\"" + preferredName + "\",\"sub\":\"" + subject
                + "\",\"home_oid\":\"" + homeObjectId + "\",\"name\":\"" + name + "\"}";

        return String.format("%s.%s.", new String(Base64.encode(idTokenHeader.getBytes(
                Charset.forName(MSALUtils.ENCODING_UTF8)), Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE)),
                new String(Base64.encode(claims.getBytes(Charset.forName(MSALUtils.ENCODING_UTF8)),
                        Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE)));
    }

    static InputStream createInputStream(final String input) {
        return input == null ? null : new ByteArrayInputStream(input.getBytes());
    }

    static URL getValidRequestUrl() throws MalformedURLException {
        return new URL(DEFAULT_AUTHORITY);
    }

    static String getSuccessResponseWithNoRefreshToken(final String idToken) {
        final String tokenResponse = "{\"id_token\":\""
                + idToken
                + "\",\"access_token\":\"" + ACCESS_TOKEN + "\",\"token_type\":\"Bearer\",\"expires_in\":\"10\",\"expires_on\":\"1368768616\",\"scope\":\"scope1 scope2\"}";
        return tokenResponse;
    }

    static String getSuccessResponseWithNoAccessToken() {
        final String tokenResponse = "{\"id_token\":\""
                + TEST_IDTOKEN
                + "\",\"token_type\":\"Bearer\",\"expires_in\":\"3600\",\"expires_on\":\"1368768616\",\"scope\":\"scope1 scope2\"}";
        return tokenResponse;
    }

    static String getSuccessResponse(final String idToken, final String accessToken, final String scopes) {
        final String tokenResponse = "{\"id_token\":\""
                + idToken
                + "\",\"access_token\":\"" + accessToken + "\", \"token_type\":\"Bearer\",\"refresh_token\":\"" + REFRESH_TOKEN + "\","
                + "\"expires_in\":\"3600\",\"expires_on\":\"1368768616\",\"scope\":\"" + scopes + "\"}";
        return tokenResponse;
    }

    static String getErrorResponseMessage(final String errorCode) {
        final String errorDescription = "\"error_description\":\"AADSTS70000: Authentication failed. Refresh Token is not valid.\r\nTrace ID: "
                + "bb27293d-74e4-4390-882b-037a63429026\r\nCorrelation ID: b73106d5-419b-4163-8bc6-d2c18f1b1a13\r\nTimestamp: 2014-11-06 18:39:47Z\","
                + "\"error_codes\":[70000, 70001],\"timestamp\":\"2014-11-06 18:39:47Z\",\"trace_id\":\"bb27293d-74e4-4390-882b-037a63429026\","
                + "\"correlation_id\":\"b73106d5-419b-4163-8bc6-d2c18f1b1a13\",\"submit_url\":null,\"context\":null";

        if (errorCode != null) {
            return "{\"error\":\"" + errorCode + "\"," + errorDescription + "}";
        }

        return "{" + errorDescription + "}";
    }

    static String encodeProtocolState(final String authority, final Set<String> scopes) throws UnsupportedEncodingException {
        String state = String.format("a=%s&r=%s", MSALUtils.urlEncode(authority),
                MSALUtils.urlEncode(MSALUtils.convertSetToString(scopes, " ")));
        return Base64.encodeToString(state.getBytes(Charset.forName("UTF-8")), Base64.NO_PADDING | Base64.URL_SAFE);
    }

    static Date getExpiredDate() {
        return getExpirationDate(-TOKEN_EXPIRATION_IN_MINUTES);
    }

    static Date getValidExpiresOn() {
        return getExpirationDate(TOKEN_EXPIRATION_IN_MINUTES);
    }

    static Date getExpirationDate(int tokenExpiredDateInMinuite) {
        final Calendar expiredTime = new GregorianCalendar();
        // access token is only valid for a hour
        expiredTime.add(Calendar.MINUTE, tokenExpiredDateInMinuite);

        return expiredTime.getTime();
    }

    static void removeAllTokens(final Context appContext) {
        final SharedPreferences accessTokenSharedPreference = getAccessTokenSharedPreference(appContext);
        final SharedPreferences.Editor accessTokenSharedPreferenceEditor = accessTokenSharedPreference.edit();
        accessTokenSharedPreferenceEditor.clear();
        accessTokenSharedPreferenceEditor.apply();

        final SharedPreferences refreshTokenSharedPreference = getRefreshTokenSharedPreference(appContext);
        final SharedPreferences.Editor refreshTokenSharedPreferenceEditor = refreshTokenSharedPreference.edit();
        refreshTokenSharedPreferenceEditor.clear();
        refreshTokenSharedPreferenceEditor.apply();
    }

    static List<AccessTokenCacheItem> getAllAccessTokens(final Context appContext) {
        final TokenCacheAccessor accessor = new TokenCacheAccessor(appContext);
        return accessor.getAllAccessTokens();
    }

    static List<RefreshTokenCacheItem> getAllRefreshTokens(final Context appContext) {
        final TokenCacheAccessor accessor = new TokenCacheAccessor(appContext);
        return accessor.getAllRefreshTokens();
    }

    static String getRawIdToken(final String displaybleId, final String uniqueId, final String homeOID) {
        return AndroidTestUtil.createIdToken(AUDIENCE, ISSUER, NAME, uniqueId, displaybleId, SUBJECT, TENANT_ID,
                VERSION, homeOID);
    }

    static SharedPreferences getAccessTokenSharedPreference(final Context appContext) {
        return appContext.getSharedPreferences(ACCESS_TOKEN_SHARED_PREFERENCE,
                Activity.MODE_PRIVATE);
    }

    static SharedPreferences getRefreshTokenSharedPreference(final Context appContext) {
        return appContext.getSharedPreferences(REFRESH_TOKEN_SHARED_PREFERENCE,
                Activity.MODE_PRIVATE);
    }

    static String getSuccessTenantDiscoveryResponse(final String authorizeEndpoint, final String tokenEndpoint) {
        return "{\"authorization_endpoint\":\"" + authorizeEndpoint + "\",\"token_endpoint\":\"" + tokenEndpoint + "\""
                + ",\"issuer\":\"some issuer\"}";
    }

    static String getSuccessInstanceDiscoveryResponse(final String tenantDiscoveryEnpdoint) {
        return "{\"tenant_discovery_endpoint\":\"" + tenantDiscoveryEnpdoint + "\"}";
    }
}
