package com.microsoft.identity.client;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tests for {@link AuthenticationRequestParameters}.
 */
public final class AuthenticationRequestParametersTest {
    static final Authority AUTHORITY = Authority.createAuthority(Util.VALID_AUTHORITY, false);
    static final TokenCache TOKEN_CACHE = new TokenCache();
    static final Set<String> SCOPE = new HashSet<>();
    static final String CLIENT_ID = "some-client-id";
    static final String REDIRECT_URI = "some://redirect.uri";
    static final Settings SETTINGS = new Settings();
    static final String LOGIN_HINT = "someLoginHint";
    static final UUID CORRELATION_ID = UUID.randomUUID();

    @Test(expected = IllegalArgumentException.class)
    public void testNullCorrelationId() {
        new AuthenticationRequestParameters(AUTHORITY, TOKEN_CACHE, SCOPE, CLIENT_ID, REDIRECT_URI, "", true, LOGIN_HINT, "",
                UIOptions.SELECT_ACCOUNT, null, SETTINGS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullScope() {
        new AuthenticationRequestParameters(AUTHORITY, TOKEN_CACHE, null, CLIENT_ID, REDIRECT_URI, "", true, LOGIN_HINT, "",
                UIOptions.SELECT_ACCOUNT, CORRELATION_ID, SETTINGS);
    }

    @Test
    public void testAuthenticationRequestParameterHappyPath() {
        final AuthenticationRequestParameters authRequestParameter = new AuthenticationRequestParameters(AUTHORITY, TOKEN_CACHE,
                SCOPE, CLIENT_ID, REDIRECT_URI, "", true, LOGIN_HINT, "", UIOptions.SELECT_ACCOUNT, CORRELATION_ID, SETTINGS);
        Assert.assertTrue(authRequestParameter.getAuthority().getAuthority().toString().equals(Util.VALID_AUTHORITY));
        Assert.assertTrue(authRequestParameter.getScope().isEmpty());
        Assert.assertTrue(authRequestParameter.getClientId().equals(CLIENT_ID));
        Assert.assertTrue(authRequestParameter.getRedirectUri().equals(REDIRECT_URI));
        Assert.assertTrue(authRequestParameter.getPolicy().isEmpty());
        Assert.assertTrue(authRequestParameter.getLoginHint().equals(LOGIN_HINT));
        Assert.assertTrue(authRequestParameter.getRestrictToSingleUser());
        Assert.assertTrue(authRequestParameter.getUIOption().equals(UIOptions.SELECT_ACCOUNT));
        Assert.assertTrue(authRequestParameter.getCorrelationId().toString().equals(CORRELATION_ID.toString()));
    }
}
