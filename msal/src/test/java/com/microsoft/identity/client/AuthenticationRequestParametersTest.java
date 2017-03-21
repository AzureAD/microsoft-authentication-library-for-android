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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tests for {@link AuthenticationRequestParameters}.
 */
public final class AuthenticationRequestParametersTest {
    static final Authority AUTHORITY = Authority.createAuthority(Util.VALID_AUTHORITY, false);
    static final TokenCache TOKEN_CACHE = Mockito.mock(TokenCache.class);
    static final Set<String> SCOPE = new HashSet<>();
    static final String CLIENT_ID = "some-client-id";
    static final String REDIRECT_URI = "some://redirect.uri";
    static final String LOGIN_HINT = "someLoginHint";
    static final UUID CORRELATION_ID = UUID.randomUUID();
    static final String COMPONENT = "test component";

    @Test(expected = IllegalArgumentException.class)
    public void testNullCorrelationId() {
        AuthenticationRequestParameters.create(AUTHORITY, TOKEN_CACHE, SCOPE, CLIENT_ID, REDIRECT_URI, LOGIN_HINT, "",
                UIBehavior.SELECT_ACCOUNT, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullScope() {
        AuthenticationRequestParameters.create(AUTHORITY, TOKEN_CACHE, null, CLIENT_ID, REDIRECT_URI, "", LOGIN_HINT,
                UIBehavior.SELECT_ACCOUNT, new RequestContext(CORRELATION_ID, COMPONENT, Telemetry.generateNewRequestId()));
    }

    @Test
    public void testAuthenticationRequestParameterHappyPath() {
        final AuthenticationRequestParameters authRequestParameter = AuthenticationRequestParameters.create(AUTHORITY, TOKEN_CACHE,
                SCOPE, CLIENT_ID, REDIRECT_URI, LOGIN_HINT, "", UIBehavior.SELECT_ACCOUNT, new RequestContext(CORRELATION_ID, COMPONENT, Telemetry.generateNewRequestId()));
        Assert.assertTrue(authRequestParameter.getAuthority().getAuthority().toString().equals(Util.VALID_AUTHORITY));
        Assert.assertTrue(authRequestParameter.getScope().isEmpty());
        Assert.assertTrue(authRequestParameter.getClientId().equals(CLIENT_ID));
        Assert.assertTrue(authRequestParameter.getRedirectUri().equals(REDIRECT_URI));
        Assert.assertTrue(authRequestParameter.getLoginHint().equals(LOGIN_HINT));
        Assert.assertTrue(authRequestParameter.getUiBehavior().equals(UIBehavior.SELECT_ACCOUNT));
        Assert.assertTrue(authRequestParameter.getRequestContext().getCorrelationId().toString().equals(CORRELATION_ID.toString()));
        Assert.assertTrue(authRequestParameter.getRequestContext().getComponent().equals(COMPONENT));
    }
}
