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
    static final Authority AUTHORITY = new Authority(Util.VALID_AUTHORITY, false);
    static final TokenCache TOKEN_CACHE = Mockito.mock(TokenCache.class);
    static final Set<String> SCOPE = new HashSet<>();
    static final String CLIENT_ID = "some-client-id";
    static final String REDIRECT_URI = "some://redirect.uri";
    static final String LOGIN_HINT = "someLoginHint";
    static final UUID CORRELATION_ID = UUID.randomUUID();

    @Test(expected = IllegalArgumentException.class)
    public void testNullCorrelationId() {
        AuthenticationRequestParameters.create(AUTHORITY, TOKEN_CACHE, SCOPE, CLIENT_ID, REDIRECT_URI, "", true, LOGIN_HINT, "",
                UIOptions.SELECT_ACCOUNT, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullScope() {
        AuthenticationRequestParameters.create(AUTHORITY, TOKEN_CACHE, null, CLIENT_ID, REDIRECT_URI, "", true, LOGIN_HINT, "",
                UIOptions.SELECT_ACCOUNT, CORRELATION_ID);
    }

    @Test
    public void testAuthenticationRequestParameterHappyPath() {
        final AuthenticationRequestParameters authRequestParameter = AuthenticationRequestParameters.create(AUTHORITY, TOKEN_CACHE,
                SCOPE, CLIENT_ID, REDIRECT_URI, "", true, LOGIN_HINT, "", UIOptions.SELECT_ACCOUNT, CORRELATION_ID);
        Assert.assertTrue(authRequestParameter.getAuthority().getAuthorityUrl().toString().equals(Util.VALID_AUTHORITY));
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
