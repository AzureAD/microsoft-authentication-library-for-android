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
package com.microsoft.identity.client.controllers;

import android.content.Intent;

import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationConfiguration;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.PkceChallenge;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.ui.AuthorizationStrategyFactory;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class LocalMSALController extends MSALController {

    private AuthorizationStrategy mAuthorizationStrategy = null;

    @Override
    public AuthenticationResult acquireToken(MSALAcquireTokenOperationParameters parameters) throws ExecutionException, InterruptedException, ClientException {


        //1) Get oAuth2Strategy for Authority Type
        OAuth2Strategy oAuth2Strategy = parameters.getAuthority().createOAuth2Strategy();

        //2) Gather authorization interactively
        AuthorizationResult result = performAuthorizationRequest(oAuth2Strategy, parameters);

        if (result.getAuthorizationStatus().equals(AuthorizationStatus.SUCCESS)) {
            //3) Exchange authorization code for token
            TokenResult tokenResult = performTokenRequest(oAuth2Strategy, result.getAuthorizationResponse(), parameters);
            if (tokenResult != null && tokenResult.getSuccess()) {
                //4) Save tokens in token cache
                //saveTokens(oAuth2Strategy, getAuthorizationRequest(oAuth2Strategy, parameters), tokenResult.getTokenResponse(), parameters.getTokenCache());
            }
        }

        throw new UnsupportedOperationException();
    }

    private AuthorizationResult performAuthorizationRequest(OAuth2Strategy strategy, MSALAcquireTokenOperationParameters parameters) throws ExecutionException, InterruptedException, ClientException {

        //TODO: Replace with factory to create the correct Authorization Strategy based on device capabilities and configuration
        mAuthorizationStrategy = AuthorizationStrategyFactory.getInstance().getAuthorizationStrategy(parameters.getActivity(), AuthorizationConfiguration.getInstance());

        Future<AuthorizationResult> future = strategy.requestAuthorization(getAuthorizationRequest(parameters), mAuthorizationStrategy);


        //We could implement Timeout Here if we wish instead of blocking indefinitely
        //future.get(10, TimeUnit.MINUTES);  // Need to handle timeout exception in the scenario it doesn't return within a reasonable amount of time
        AuthorizationResult result = future.get();

        return result;

    }

    private AuthorizationRequest getAuthorizationRequest(MSALAcquireTokenOperationParameters parameters) {
        MicrosoftStsAuthorizationRequest.Builder builder = new MicrosoftStsAuthorizationRequest.Builder(
                parameters.getClientId(),
                parameters.getRedirectUri(),
                StringUtil.join(' ', parameters.getScopes()));

        builder.setResponseType(AuthorizationRequest.ResponseType.CODE);

        return builder.build();
    }

    private TokenResult performTokenRequest(OAuth2Strategy strategy, AuthorizationResponse response, MSALAcquireTokenOperationParameters parameters) {

        TokenRequest tokenRequest = new TokenRequest();

        tokenRequest.setCode(response.getCode());
        tokenRequest.setClientId(parameters.getClientId());
        tokenRequest.setRedirectUri(parameters.getRedirectUri());
        tokenRequest.setScope(StringUtil.join(' ', parameters.getScopes()));
        tokenRequest.setGrantType(TokenRequest.GrantTypes.AUTHORIZATION_CODE);

        TokenResult tokenResult = null;

        try {
            tokenResult = strategy.requestToken(tokenRequest);
        } catch (IOException e) {
            //TODO: Figure out exception handling
        }

        return tokenResult;

    }

/*
    private void saveTokens(OAuth2Strategy strategy, AuthorizationRequest request, TokenResponse tokenResponse, MsalOAuth2TokenCache tokenCache){
        try {
            tokencCache.saveTokens(mOAuthStrategy, authRequest, tokenResult.getTokenResponse());
        } catch (ClientException e) {
            e.printStackTrace();
        }
    }
*/


    @Override
    public void completeAcquireToken(int requestCode, int resultCode, final Intent data) {
        mAuthorizationStrategy.completeAuthorization(requestCode, resultCode, data);
    }

    @Override
    public AuthenticationResult acquireTokenSilent(MSALAcquireTokenSilentOperationParameters request) {
        throw new UnsupportedOperationException();
    }
}
