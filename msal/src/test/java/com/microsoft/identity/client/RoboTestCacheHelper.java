package com.microsoft.identity.client;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.internal.testutils.authorities.MockAuthority;

import org.mockito.Mockito;

public class RoboTestCacheHelper {

    public static ICacheRecord saveTokens(TokenResponse tokenResponse, IPublicClientApplication application) throws ClientException {
        final OAuth2TokenCache tokenCache = application.getConfiguration().getOAuth2TokenCache();
        final String clientId = application.getConfiguration().getClientId();
        final Authority authority = new MockAuthority();
        final OAuth2Strategy strategy = authority.createOAuth2Strategy();
        final MicrosoftStsAuthorizationRequest fakeAuthRequest = Mockito.mock(MicrosoftStsAuthorizationRequest.class);
        Mockito.when(fakeAuthRequest.getAuthority()).thenReturn(authority.getAuthorityURL());
        Mockito.when(fakeAuthRequest.getClientId()).thenReturn(clientId);
        return tokenCache.save(strategy, fakeAuthRequest, tokenResponse);
    }
}
