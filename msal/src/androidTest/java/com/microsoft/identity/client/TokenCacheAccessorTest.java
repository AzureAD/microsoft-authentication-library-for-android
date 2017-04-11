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

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;

/**
 * Tests for {@link TokenCacheAccessor}.
 */
@RunWith(AndroidJUnit4.class)
public final class TokenCacheAccessorTest extends AndroidTestCase {
    private TokenCacheAccessor mAccessor;
    private Context mAppContext;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        InstrumentationRegistry.getContext().getCacheDir();
        System.setProperty("dexmaker.dexcache",
                InstrumentationRegistry.getContext().getCacheDir().getPath());

        mAppContext = InstrumentationRegistry.getContext().getApplicationContext();
        mAccessor = new TokenCacheAccessor(mAppContext);
        AndroidTestUtil.removeAllTokens(mAppContext);
        Telemetry.disableForTest(true);
    }

    @After
    public void tearDown() {
        AndroidTestUtil.removeAllTokens(mAppContext);
        assertTrue(mAccessor.getAllAccessTokens(Telemetry.generateNewRequestId()).size() == 0);
        assertTrue(mAccessor.getAllRefreshTokens(Telemetry.generateNewRequestId()).size() == 0);
        Telemetry.disableForTest(false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTokenCacheAccessorConstructor() {
        new TokenCacheAccessor(null);
    }

    /**
     * Verify that access token is stored with the passed in cache key.
     */
    @Test
    public void testSaveAT() throws MsalException {
        final Set<String> scopes = new HashSet<>();
        scopes.add("scope1");

        final String accessTokenKey1 = "access-token-key1";
        final String accessToken1 = "access-token-1";
        mAccessor.saveAccessToken(accessTokenKey1, accessToken1, AndroidTestUtil.getTestRequestContext());

        // verify the access token is saved
        assertTrue(mAccessor.getAllAccessTokens(Telemetry.generateNewRequestId()).size() == 1);

        final String accessTokenKey2 = "access-token-key2";
        final String accessToken2 = "access-token-2";
        mAccessor.saveAccessToken(accessTokenKey2, accessToken2, AndroidTestUtil.getTestRequestContext());

        // verify there are two access token entries in the case
        assertTrue(mAccessor.getAllAccessTokens(Telemetry.generateNewRequestId()).size() == 2);
    }

    /**
     * Verify that RT is saved correctly for single user case.
     */
    @Test
    public void testSaveRT() throws MsalException {
        final String refreshTokenKey1 = "refresh-token-key1";
        final String refreshToken1 = "refresh-token2";
        mAccessor.saveRefreshToken(refreshTokenKey1, refreshToken1, AndroidTestUtil.getTestRequestContext());

        assertTrue(mAccessor.getAllRefreshTokens(Telemetry.generateNewRequestId()).size() == 1);
    }

    @Test
    public void testDeleteRTItems() {
        final String refreshTokenKey = "refresh-token-key1";
        final String refreshToken1 = "refresh-token1";
        mAccessor.saveRefreshToken(refreshTokenKey, refreshToken1, AndroidTestUtil.getTestRequestContext());

        assertTrue(mAccessor.getAllRefreshTokens(Telemetry.generateNewRequestId()).size() == 1);

        mAccessor.deleteRefreshToken(refreshTokenKey, AndroidTestUtil.getTestRequestContext());
        assertTrue(mAccessor.getAllRefreshTokens(Telemetry.generateNewRequestId()).size() == 0);
    }

    @Test
    public void testDeleteATItems() {
        final String accessTokenKey = "access-token-key";
        final String accessToken = "access-token";
        mAccessor.saveAccessToken(accessTokenKey, accessToken, AndroidTestUtil.getTestRequestContext());

        assertTrue(mAccessor.getAllAccessTokens(Telemetry.generateNewRequestId()).size() == 1);

        mAccessor.deleteAccessToken(accessTokenKey, AndroidTestUtil.getTestRequestContext());
        assertTrue(mAccessor.getAllAccessTokens(Telemetry.generateNewRequestId()).size() == 0);
    }
}