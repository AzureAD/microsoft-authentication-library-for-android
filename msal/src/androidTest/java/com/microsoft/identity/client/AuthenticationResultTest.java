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
import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests for {@link AuthorizationResult}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class AuthenticationResultTest {
    static final String ACCESS_TOKEN = "access token";
    static final String REFRESH_TOKEN = "refresh token";
    static final Date EXPIRES_ON = new Date();
    static final String TOKEN_TYPE = "token type";
    static final String SCOPE = "scope1 scope2 scope3";
    static final int SCOPE_LENGTH = 3;
    static final int TIME_OFFSET = 50;

    /**
     * Verify scope is correctly constructed if token response contains the empty scope.
     */
    @Test
    public void testTokenResponseContainEmptyScope() throws AuthenticationException {
        final TokenResponse tokenResponse = new TokenResponse(ACCESS_TOKEN, "", REFRESH_TOKEN, EXPIRES_ON, EXPIRES_ON,
                EXPIRES_ON, "", TOKEN_TYPE, null);

        final AuthenticationResult authenticationResult = AuthenticationResult.create(new SuccessTokenResponse(tokenResponse));
        verifyScopeIsEmpty(authenticationResult);
    }

    /**
     * Verify scope is correctly constructed if token response contains null scope.
     */
    @Test
    public void testTokenResponseContainNullScope() throws AuthenticationException {
        final TokenResponse tokenResponse = new TokenResponse(ACCESS_TOKEN, "", REFRESH_TOKEN, EXPIRES_ON, EXPIRES_ON,
                EXPIRES_ON, null, TOKEN_TYPE, null);

        final AuthenticationResult authenticationResult = AuthenticationResult.create(new SuccessTokenResponse(tokenResponse));
        verifyScopeIsEmpty(authenticationResult);
    }

    /**
     * Verify scope is correctly constructed if token response contains the scope with multiple spaces in the middle and
     * trailing spaces in the end.
     */
    @Test
    public void testTokenResponseContainsScopeWithTrailingSpace() throws AuthenticationException {
        final String scopes = " scope1 scope2  scope3   ";
        final TokenResponse tokenResponse = new TokenResponse(ACCESS_TOKEN, "", REFRESH_TOKEN, EXPIRES_ON, EXPIRES_ON,
                EXPIRES_ON, scopes, TOKEN_TYPE, null);

        final AuthenticationResult authenticationResult = AuthenticationResult.create(new SuccessTokenResponse(tokenResponse));
        final String[] scopeArray = authenticationResult.getScope();
        Assert.assertNotNull(scopeArray);
        Assert.assertTrue(scopeArray.length == SCOPE_LENGTH);
        Set<String> scopeSet = new HashSet<>(Arrays.asList(scopeArray));
        Assert.assertTrue(scopeSet.contains("scope1"));
        Assert.assertTrue(scopeSet.contains("scope2"));
        Assert.assertTrue(scopeSet.contains("scope3"));
    }

    /**
     * Verify that if both AT and Id Token are returned, AT will be used as the token returned in authenticationResult.
     */
    @Test
    public void testBothATAndIdTokenReturned() throws AuthenticationException {
        final Date expiresOn = getExpiresOn(TIME_OFFSET);
        final Date idTokenExpiresOn = getExpiresOn(-TIME_OFFSET);

        final TokenResponse tokenResponse = new TokenResponse(ACCESS_TOKEN, AndroidTestUtil.TEST_IDTOKEN,
                REFRESH_TOKEN, expiresOn, idTokenExpiresOn, EXPIRES_ON, SCOPE, TOKEN_TYPE, null);
        final AuthenticationResult authenticationResult = AuthenticationResult.create(new SuccessTokenResponse(tokenResponse));
        Assert.assertTrue(authenticationResult.getToken().equals(ACCESS_TOKEN));
        Assert.assertTrue(authenticationResult.getExpiresOn().equals(expiresOn));
    }

    /**
     * Verify that if only Id token is returned, it will be returned as token in authenticationResult, idtoken expiresOn
     * will also be returned.
     */
    @Test
    public void testOnlyIdTokenReturned() throws AuthenticationException {
        final Date expiresOn = getExpiresOn(TIME_OFFSET);
        final Date idTokenExpiresOn = getExpiresOn(-TIME_OFFSET);
        final TokenResponse tokenResponse = new TokenResponse(null, AndroidTestUtil.TEST_IDTOKEN,
                REFRESH_TOKEN, expiresOn, idTokenExpiresOn, EXPIRES_ON, SCOPE, TOKEN_TYPE, null);
        final AuthenticationResult authenticationResult = AuthenticationResult.create(new SuccessTokenResponse(tokenResponse));

        Assert.assertTrue(authenticationResult.getToken().equals(AndroidTestUtil.TEST_IDTOKEN));
        Assert.assertTrue(authenticationResult.getExpiresOn().equals(idTokenExpiresOn));
    }

    @Test
    public void testHomeOidNotReturned() throws UnsupportedEncodingException, AuthenticationException {
        final String uniqueId = "unique";
        final TokenResponse tokenResponse = new TokenResponse(null, PublicClientApplicationTest.getIdToken("displayable",
                uniqueId, ""), REFRESH_TOKEN, new Date(), new Date(), new Date(), SCOPE, TOKEN_TYPE, null);
        final AuthenticationResult result = AuthenticationResult.create(new SuccessTokenResponse(tokenResponse));

        final User user = result.getUser();
        Assert.assertNotNull(user);
        Assert.assertTrue(user.getHomeObjectId().equals(uniqueId));
    }

    private void verifyScopeIsEmpty(final AuthenticationResult authenticationResult) {
        Assert.assertNotNull(authenticationResult.getScope());
        Assert.assertTrue(authenticationResult.getScope().length == 0);
    }

    private Date getExpiresOn(int offset) {
        final Calendar idTokenCalendar = new GregorianCalendar();
        idTokenCalendar.add(Calendar.SECOND, -offset);

        return idTokenCalendar.getTime();
    }
}
