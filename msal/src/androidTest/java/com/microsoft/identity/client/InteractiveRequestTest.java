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
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Test for {@link InteractiveRequest}.
 */
@RunWith(AndroidJUnit4.class)
public final class InteractiveRequestTest extends AndroidTestCase {
    static final String AUTHORITY = "https://login.microsoftonline.com/common";
    static final String CLIENT_ID = "client-id";
    static final String POLICY = "signin signup";
    static final UUID CORRELATION_ID = UUID.randomUUID();
    static final String LOGIN_HINT = "test@test.onmicrosoft.com";

    private Context mAppContext;
    private String mRedirectUri;

    @Before
    public void setUp() throws Exception{
        super.setUp();
        InstrumentationRegistry.getContext().getCacheDir();
        System.setProperty("dexmaker.dexcache", InstrumentationRegistry.getContext().getCacheDir().getPath());

        mAppContext = InstrumentationRegistry.getContext().getApplicationContext();
        mRedirectUri = "msauth-client-id://" + mAppContext.getPackageName();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullScope() {
        new InteractiveRequest(Mockito.mock(Activity.class), getAuthRequestParameters(
                null, mRedirectUri, LOGIN_HINT, UIOptions.FORCE_LOGIN), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorEmptyScope() {
        new InteractiveRequest(Mockito.mock(Activity.class), getAuthRequestParameters(
                Collections.<String>emptySet(), mRedirectUri, LOGIN_HINT, UIOptions.FORCE_LOGIN), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorScopeContainsReservedScope() {
        final Set<String> scopes = new HashSet<>();
        scopes.add(OauthConstants.Oauth2Value.SCOPE_EMAIL);
        scopes.add(OauthConstants.Oauth2Value.SCOPE_PROFILE);

        new InteractiveRequest(Mockito.mock(Activity.class), getAuthRequestParameters(scopes, mRedirectUri,
                LOGIN_HINT, UIOptions.FORCE_LOGIN), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorClientIdIsNotSingleScope() {
        final Set<String> scopes = getScopesContainsReservedScope();

        new InteractiveRequest(Mockito.mock(Activity.class), getAuthRequestParameters(scopes, mRedirectUri,
                LOGIN_HINT, UIOptions.FORCE_LOGIN), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorEmptyRedirectUri() {
        new InteractiveRequest(Mockito.mock(Activity.class), getAuthRequestParameters(getScopes(), "", LOGIN_HINT,
                UIOptions.FORCE_LOGIN), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAdditionalScopeContainsReservedScope() {
        final Set<String> additionalScopes = getScopesContainsReservedScope();
        new InteractiveRequest(Mockito.mock(Activity.class), getAuthRequestParameters(getScopes(), "", LOGIN_HINT,
                UIOptions.FORCE_LOGIN), additionalScopes.toArray(new String[additionalScopes.size()]));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoginHintNotSetUIOptionIsAsCurrentUser() {
        new InteractiveRequest(Mockito.mock(Activity.class), getAuthRequestParameters(getScopes(), "", "",
                UIOptions.ACT_AS_CURRENT_USER), null);
    }

    @Test
    public void testInteractiveRequestNotLoadFromCache() {
        final InteractiveRequest interactiveRequest = new InteractiveRequest(Mockito.mock(Activity.class),
                getAuthRequestParameters(getScopes(), mRedirectUri, LOGIN_HINT, UIOptions.ACT_AS_CURRENT_USER), null);
        Assert.assertFalse(interactiveRequest.mLoadFromCache);
    }

    @Test
    public void testGetAuthorizationUriWithPolicyUIOptionIsActAsCurrentUser() throws UnsupportedEncodingException {
        final InteractiveRequest interactiveRequest = new InteractiveRequest(Mockito.mock(Activity.class),
                getAuthenticationParams(POLICY, UIOptions.ACT_AS_CURRENT_USER), null);
        final String actualAuthorizationUri = interactiveRequest.getAuthorizationUri();
        final Uri authorityUrl = Uri.parse(actualAuthorizationUri);
        Map<String, String> queryStrings = MSALUtils.decodeUrlToMap(authorityUrl.getQuery(), "&");

        assertTrue(MSALUtils.convertSetToString(getExpectedScopes(true), " ").equals(
                queryStrings.get(OauthConstants.Oauth2Parameters.SCOPE)));
        assertTrue("true".equals(queryStrings.get(OauthConstants.Oauth2Parameters.RESTRICT_TO_HINT)));
        verifyCommonQueryString(queryStrings);
    }

    @Test
    public void testGetAuthorizationUriNoPolicyUIOptionForceLogin() throws UnsupportedEncodingException {
        final String[] additionalScope = {"additionalScope"};
        final InteractiveRequest interactiveRequest = new InteractiveRequest(Mockito.mock(Activity.class),
                getAuthenticationParams("", UIOptions.FORCE_LOGIN), additionalScope);
        final String actualAuthorizationUri = interactiveRequest.getAuthorizationUri();
        final Uri authorityUrl = Uri.parse(actualAuthorizationUri);
        Map<String, String> queryStrings = MSALUtils.decodeUrlToMap(authorityUrl.getQuery(), "&");

        final Set<String> expectedScopes = getExpectedScopes(false);
        expectedScopes.add("additionalScope");
        assertTrue(MSALUtils.convertSetToString(expectedScopes, " ").equals(
                queryStrings.get(OauthConstants.Oauth2Parameters.SCOPE)));
        assertTrue(OauthConstants.PromptValue.LOGIN.equals(queryStrings.get(OauthConstants.Oauth2Parameters.PROMPT)));
        verifyCommonQueryString(queryStrings);
    }

    private AuthenticationRequestParameters getAuthenticationParams(final String policy, final UIOptions uiOptions) {
        return new AuthenticationRequestParameters(new Authority(AUTHORITY, false), new TokenCache(), getScopes(),
                CLIENT_ID, mRedirectUri, policy, true, LOGIN_HINT, "", uiOptions, CORRELATION_ID, new Settings());
    }

    private AuthenticationRequestParameters getAuthRequestParameters(final Set<String> scopes,
                                                                     final String redirectUri,
                                                                     final String loginHint,
                                                                     final UIOptions uiOptions) {
        return new AuthenticationRequestParameters(new Authority(AUTHORITY, false), new TokenCache(), scopes,
                CLIENT_ID, redirectUri, POLICY, true, loginHint, "", uiOptions, CORRELATION_ID, new Settings());
    }

    private Set<String> getScopes() {
        final String[] scopes = {"scope1", "scope2"};
        return new TreeSet<>(Arrays.asList(scopes));
    }

    private Set<String> getExpectedScopes(boolean withPolicy) {
        final Set<String> scopes = getScopes();
        if (withPolicy) {
            scopes.add("offline_access");
            scopes.add("openid");
        } else {
            scopes.addAll(new TreeSet<>(Arrays.asList(OauthConstants.Oauth2Value.RESERVED_SCOPES)));
        }

        return scopes;
    }

    private Set<String> getScopesContainsReservedScope() {
        final Set<String> scopes = new HashSet<>();
        scopes.add(CLIENT_ID);
        scopes.add("scope");

        return scopes;
    }

    private void verifyCommonQueryString(final Map<String, String> queryStrings) {
        assertTrue(CLIENT_ID.equals(queryStrings.get(OauthConstants.Oauth2Parameters.CLIENT_ID)));
        assertTrue(mRedirectUri.equals(queryStrings.get(OauthConstants.Oauth2Parameters.REDIRECT_URI)));
        assertTrue(OauthConstants.Oauth2ResponseType.CODE.equals(queryStrings.get(
                OauthConstants.Oauth2Parameters.RESPONSE_TYPE)));
        assertTrue(CORRELATION_ID.toString().equals(queryStrings.get(OauthConstants.OauthHeader.CORRELATION_ID)));
        assertTrue(LOGIN_HINT.equals(queryStrings.get(OauthConstants.Oauth2Parameters.LOGIN_HINT)));
    }
}
