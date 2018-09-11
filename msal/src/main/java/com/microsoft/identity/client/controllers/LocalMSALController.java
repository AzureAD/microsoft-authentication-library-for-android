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

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.MsalClientException;
import com.microsoft.identity.client.authorities.Authority;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationConfiguration;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.ui.AuthorizationStrategyFactory;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class LocalMSALController extends MSALController {

    private AuthorizationStrategy mAuthorizationStrategy = null;
    private AuthorizationRequest mAuthorizationRequest = null;

    @Override
    public AcquireTokenResult acquireToken(MSALAcquireTokenOperationParameters parameters) throws ExecutionException, InterruptedException, ClientException, IOException, MsalClientException {

        AcquireTokenResult acquireTokenResult = new AcquireTokenResult();

        //0) Get known authority result
        throwIfNetworkNotAvailable(parameters.getAppContext());
        Authority.KnownAuthorityResult authorityResult = Authority.getKnownAuthorityResult(parameters.getAuthority());

        //0.1 If not known throw resulting exception
        if(!authorityResult.getKnown()){
            throw authorityResult.getMsalClientException();
        }

        //1) Get oAuth2Strategy for Authority Type
        OAuth2Strategy oAuth2Strategy = parameters.getAuthority().createOAuth2Strategy();

        //2) Request authorization interactively
        AuthorizationResult result = performAuthorizationRequest(oAuth2Strategy, parameters);
        acquireTokenResult.setAuthorizationResult(result);

        if (result.getAuthorizationStatus().equals(AuthorizationStatus.SUCCESS)) {
            //3) Exchange authorization code for token
            TokenResult tokenResult = performTokenRequest(oAuth2Strategy, mAuthorizationRequest, result.getAuthorizationResponse(), parameters);
            acquireTokenResult.setTokenResult(tokenResult);
            if (tokenResult != null && tokenResult.getSuccess()) {
                //4) Save tokens in token cache
                ICacheRecord cacheRecord = saveTokens(oAuth2Strategy, mAuthorizationRequest, tokenResult.getTokenResponse(), parameters.getTokenCache());
                acquireTokenResult.setAuthenticationResult(new AuthenticationResult(cacheRecord));
            }
        }

        return acquireTokenResult;

    }

    private AuthorizationResult performAuthorizationRequest(OAuth2Strategy strategy, MSALAcquireTokenOperationParameters parameters) throws ExecutionException, InterruptedException, MsalClientException {

        throwIfNetworkNotAvailable(parameters.getAppContext());

        mAuthorizationStrategy = AuthorizationStrategyFactory.getInstance().getAuthorizationStrategy(parameters.getActivity(), AuthorizationConfiguration.getInstance());
        mAuthorizationRequest = getAuthorizationRequest(strategy, parameters);

        Future<AuthorizationResult> future = strategy.requestAuthorization(mAuthorizationRequest, mAuthorizationStrategy);

        //We could implement Timeout Here if we wish instead of blocking indefinitely
        //future.get(10, TimeUnit.MINUTES);  // Need to handle timeout exception in the scenario it doesn't return within a reasonable amount of time
        AuthorizationResult result = future.get();

        return result;

    }

    private AuthorizationRequest getAuthorizationRequest(OAuth2Strategy strategy, MSALAcquireTokenOperationParameters parameters) {

        AuthorizationRequest.Builder builder = strategy.createAuthorizationRequestBuilder();

        List<String> msalScopes = new ArrayList<>();
        msalScopes.add("openid");
        msalScopes.add("profile");
        msalScopes.add("offline_access");
        msalScopes.addAll(parameters.getScopes());
        msalScopes.addAll(parameters.getExtraScopesToConsent());

        AuthorizationRequest request = builder
                .setClientId(parameters.getClientId())
                .setRedirectUri(parameters.getRedirectUri())
                .setScope(StringUtil.join(' ', msalScopes))
                .setLoginHint(parameters.getLoginHint())
                .setExtraQueryParam(parameters.getExtraQueryStringParameters())
                .setPrompt(parameters.getUIBehavior().toString())
                .build();

        return request;
    }

    private TokenResult performTokenRequest(OAuth2Strategy strategy, AuthorizationRequest request, AuthorizationResponse response, MSALAcquireTokenOperationParameters parameters) throws IOException, MsalClientException {

        throwIfNetworkNotAvailable(parameters.getAppContext());

        TokenRequest tokenRequest = strategy.createTokenRequest(request, response);
        tokenRequest.setGrantType(TokenRequest.GrantTypes.AUTHORIZATION_CODE);

        TokenResult tokenResult = null;

        tokenResult = strategy.requestToken(tokenRequest);

        return tokenResult;
    }

    void throwIfNetworkNotAvailable(Context context) throws MsalClientException {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            throw new MsalClientException(MsalClientException.DEVICE_NETWORK_NOT_AVAILABLE, "Device network connection is not available.");
        }
    }

    private ICacheRecord saveTokens(OAuth2Strategy strategy, AuthorizationRequest request, TokenResponse tokenResponse, OAuth2TokenCache tokenCache) throws ClientException {
        return tokenCache.save(strategy, request, tokenResponse);
    }


    @Override
    public void completeAcquireToken(int requestCode, int resultCode, final Intent data) {
        mAuthorizationStrategy.completeAuthorization(requestCode, resultCode, data);
    }

    @Override
    public AcquireTokenResult acquireTokenSilent(MSALAcquireTokenSilentOperationParameters request) {
        throw new UnsupportedOperationException();
    }
}
