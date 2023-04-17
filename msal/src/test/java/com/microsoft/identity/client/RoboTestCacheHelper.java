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

import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.authorities.AccountsInOneOrganization;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.java.providers.oauth2.TokenResponse;
import com.microsoft.identity.internal.testutils.TestConstants;
import com.microsoft.identity.internal.testutils.authorities.MockAuthority;

import org.mockito.Mockito;

public class RoboTestCacheHelper {

    // adding this method here in its own class as the getOAuth2TokenCache method has package-private
    // access inside the PubliClientApplication class. Therefore, it is required to place this method
    // in this class as part of the com.microsoft.identity.client package to be able to utilize it
    public static ICacheRecord saveTokens(TokenResponse tokenResponse, IPublicClientApplication application) throws ClientException {
        final OAuth2TokenCache tokenCache = application.getConfiguration().getOAuth2TokenCache();
        final String clientId = application.getConfiguration().getClientId();
        final Authority authority = new MockAuthority(new AccountsInOneOrganization(
                TestConstants.Authorities.AAD_MOCK_AUTHORITY,
                TestConstants.Authorities.AAD_MOCK_AUTHORITY_TENANT)
        );
        final OAuth2StrategyParameters strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(application.getConfiguration().getAppContext()))
                .build();
        final OAuth2Strategy strategy = authority.createOAuth2Strategy(strategyParameters);
        final MicrosoftStsAuthorizationRequest mockAuthRequest = Mockito.mock(MicrosoftStsAuthorizationRequest.class);
        Mockito.when(mockAuthRequest.getAuthority()).thenReturn(authority.getAuthorityURL());
        Mockito.when(mockAuthRequest.getClientId()).thenReturn(clientId);
        return tokenCache.save(strategy, mockAuthRequest, tokenResponse);
    }
}
