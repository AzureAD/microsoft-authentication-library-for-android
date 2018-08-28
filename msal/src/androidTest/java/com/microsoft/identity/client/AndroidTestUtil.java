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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Util class for instrumentation tests.
 */
public final class AndroidTestUtil {
    private static final String ACCESS_TOKEN_SHARED_PREFERENCE = "com.microsoft.identity.client.token";
    private static final String REFRESH_TOKEN_SHARED_PREFERENCE = "com.microsoft.identity.client.refreshToken";
    static final String DEFAULT_AUTHORITY_WITH_TENANT = "https://login.microsoftonline.com/tenant";
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

    static final String UID = "user-uid";
    static final String UTID = "adbc-user-tenantid";

    // Dummy client_info with randomly generated GUIDs
    private static final String MOCK_CLIENT_INFO = "eyJ1aWQiOiI1MGE0YjhhMS0zMzNiLTQwNDEtOGQzNS0wYTg2MDY2YzE1YTgiLCJ1dGlkIjoiMGE4NGU5NTctODg0Yi00NmQxLTk0OGYtYTUwMWIwZWE2NmYyIn0=";
    // The GUIDs in the above client_info
    public static final String MOCK_UID = "50a4b8a1-333b-4041-8d35-0a86066c15a8";
    public static final String MOCK_UTID = "0a84e957-884b-46d1-948f-a501b0ea66f2";

    /**
     * Private to prevent util class from being initiated.
     */
    private AndroidTestUtil() {
        // Leave as blank intentionally.
    }

    static final String TEST_IDTOKEN;

    static {
        // TODO populate the above static field with values from the block comment above
        TEST_IDTOKEN = createIdToken(
                "5a434691-ccb2-4fd1-b97b-b64bcfbc03fc",
                "https://login.microsoftonline.com/0287f963-2d72-4363-9e3a-5705c5b0f031/v2.0",
                "Mam User",
                "642915a4-d05b-4ab7-bffb-faf2b71778ef",
                "mam@msdevex.onmicrosoft.com",
                "z_MszxrmGty92ydc6UrnnIUmPv8DMpLEXyW57Fbaozo",
                "0287f963-2d72-4363-9e3a-5705c5b0f031",
                "2.0",
                new HashMap<String, Object>() {{
                    put("iat", 1471497316);
                    put("nbf", 1471497316);
                    put("exp", 1471501216);
                }}
        );
    }

    static String createIdToken(final String audience,
                                final String issuer,
                                final String name,
                                final String objectId,
                                final String preferredName,
                                final String subject,
                                final String tenantId,
                                final String version,
                                final Map<String, Object> extraClaims) {
        final SecureRandom random = new SecureRandom();
        final byte[] secret = new byte[32];
        random.nextBytes(secret);

        try {
            final JWSSigner signer = new MACSigner(secret);
            JWTClaimsSet.Builder claimsBuilder =
                    new JWTClaimsSet.Builder()
                            .audience(audience)
                            .issuer(issuer)
                            .claim("name", name)
                            .claim("oid", objectId)
                            .claim("preferred_username", preferredName)
                            .subject(subject)
                            .claim("tid", tenantId)
                            .claim("ver", version);

            if (null != extraClaims) {
                for (final Map.Entry<String, Object> claim : extraClaims.entrySet()) {
                    claimsBuilder = claimsBuilder.claim(claim.getKey(), claim.getValue());
                }
            }

            JWTClaimsSet claims = claimsBuilder.build();

            // Create the JWT
            final SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);

            // Sign it
            signedJWT.sign(signer);

            // Stringify it for testing
            return signedJWT.serialize();
        } catch (JOSEException e) {
            return null;
        }
    }

//    static String createIdToken(final String audience,
//                                final String issuer,
//                                final String name,
//                                final String objectId,
//                                final String preferredName,
//                                final String subject,
//                                final String tenantId,
//                                final String version) {
//        final String idTokenHeader = "{\"typ\":\"JWT\",\"alg\":\"RS256\"}";
//        final String claims = "{\"aud\":\"" + audience + "\",\"iss\":\"" + issuer
//                + "\",\"ver\":\"" + version + "\",\"tid\":\"" + tenantId + "\",\"oid\":\"" + objectId
//                + "\",\"preferred_username\":\"" + preferredName + "\",\"sub\":\"" + subject
//                + "\",\"name\":\"" + name + "\"}";
//
//        return String.format("%s.%s.", new String(Base64.encode(idTokenHeader.getBytes(
//                Charset.forName(MsalUtils.ENCODING_UTF8)), Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE)),
//                new String(Base64.encode(claims.getBytes(Charset.forName(MsalUtils.ENCODING_UTF8)),
//                        Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE)));
//    }

    static String createRawClientInfo(final String uid, final String utid) {
        final String claims = "{\"uid\":\"" + uid + "\",\"utid\":\"" + utid + "\"}";

        return new String(Base64.encode(claims.getBytes(
                Charset.forName(MsalUtils.ENCODING_UTF8)), Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE));
    }

    static InputStream createInputStream(final String input) {
        return input == null ? null : new ByteArrayInputStream(input.getBytes());
    }

    static URL getValidRequestUrl() throws MalformedURLException {
        return new URL(DEFAULT_AUTHORITY_WITH_TENANT);
    }

    static String getSuccessResponseWithNoRefreshToken(final String idToken) {
        final String tokenResponse = "{\"id_token\":\""
                + idToken
                + "\",\"access_token\":\"" + ACCESS_TOKEN + "\",\"token_type\":\"Bearer\",\"expires_in\":\"3600\",\"expires_on\":\"1368768616\",\"scope\":\"scope1 scope2\",\"client_info\":\"" + MOCK_CLIENT_INFO + "\"}";
        return tokenResponse;
    }

    static String getSuccessResponseWithNoAccessToken() {
        final String tokenResponse = "{\"id_token\":\""
                + TEST_IDTOKEN
                + "\",\"token_type\":\"Bearer\",\"expires_in\":\"3600\",\"expires_on\":\"1368768616\",\"scope\":\"scope1 scope2\"}";
        return tokenResponse;
    }

    static String getSuccessResponse(final String idToken, final String accessToken, final String scopes, final String clientInfo) {
        String tokenResponse = "{\"id_token\":\""
                + idToken
                + "\",\"access_token\":\"" + accessToken + "\", \"token_type\":\"Bearer\",\"refresh_token\":\"" + REFRESH_TOKEN + "\","
                + "\"expires_in\":\"3600\",\"expires_on\":\"1368768616\",\"scope\":\"" + scopes + "\""; //}";
        if (!MsalUtils.isEmpty(clientInfo)) {
            tokenResponse += ",\"client_info\":\"" + clientInfo + "\"";
        }

        tokenResponse += "}";

        return tokenResponse;
    }

    static String getSuccessResponseNoExpires(final String idToken, final String accessToken, final String scopes, final String clientInfo) {
        String tokenResponse = "{\"id_token\":\""
                + idToken
                + "\",\"access_token\":\"" + accessToken + "\", \"token_type\":\"Bearer\",\"refresh_token\":\"" + REFRESH_TOKEN + "\","
                + "\"scope\":\"" + scopes + "\"";
        if (!MsalUtils.isEmpty(clientInfo)) {
            tokenResponse += ",\"client_info\":\"" + clientInfo + "\"";
        }

        tokenResponse += "}";

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
        String state = String.format("a=%s&r=%s", MsalUtils.urlFormEncode(authority),
                MsalUtils.urlFormEncode(MsalUtils.convertSetToString(scopes, " ")));
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
        Telemetry.disableForTest(true);
        final TokenCache tokenCache = new TokenCache(appContext);
        List<AccessTokenCacheItem> accessTokenCacheItems = tokenCache.getAllAccessTokens(getTestRequestContext());
        Telemetry.disableForTest(false);
        return accessTokenCacheItems;
    }

    static List<RefreshTokenCacheItem> getAllRefreshTokens(final Context appContext) {
        Telemetry.disableForTest(true);
        final TokenCache tokenCache = new TokenCache(appContext);
        List<RefreshTokenCacheItem> refreshTokenCacheItems = tokenCache.getAllRefreshTokens(getTestRequestContext());
        Telemetry.disableForTest(false);
        return refreshTokenCacheItems;
    }

    static String getRawIdToken(final String displaybleId, final String uniqueId, final String tenantId) {
        return AndroidTestUtil.createIdToken(
                AUDIENCE,
                ISSUER,
                NAME,
                uniqueId,
                displaybleId,
                SUBJECT,
                tenantId,
                VERSION,
                null
        );
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

    static RequestContext getTestRequestContext() {
        return new RequestContext(UUID.randomUUID(), "", Telemetry.generateNewRequestId());
    }
}