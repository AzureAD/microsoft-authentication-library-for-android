package com.microsoft.identity.client.controllers;

import android.content.Intent;

import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationConfiguration;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.ui.AuthorizationStrategyFactory;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class LocalMSALController extends MSALController{



    private AuthorizationStrategy mAuthorizationStrategy = null;

    @Override
    public AuthenticationResult AcquireToken(MSALAcquireTokenOperationParameters parameters) throws ExecutionException, InterruptedException {


        //1) TODO: Use factory to get applicable oAuth and Authorization strategies
        OAuth2Strategy oAuth2Strategy = new MicrosoftStsOAuth2Strategy(new MicrosoftStsOAuth2Configuration());

        //2) Gather authorization interactively
        AuthorizationResult result = performAuthorizationRequest(oAuth2Strategy, parameters);

        if(result.getSuccess()){
            //3) Exchange authorization code for token
            TokenResult tokenResult = performTokenRequest(oAuth2Strategy, result.getAuthorizationResponse(), parameters);
            if(tokenResult != null && tokenResult.getSuccess()){
                //4) Save tokens in token cache
                //saveTokens(oAuth2Strategy, getAuthorizationRequest(oAuth2Strategy, parameters), tokenResult.getTokenResponse(), parameters.getTokenCache());
            }
        }

        throw new UnsupportedOperationException();
    }

    private AuthorizationResult performAuthorizationRequest(OAuth2Strategy strategy, MSALAcquireTokenOperationParameters parameters) throws ExecutionException, InterruptedException {

        //TODO: Replace with factory to create the correct Authorization Strategy based on device capabilities and configuration

        mAuthorizationStrategy = AuthorizationStrategyFactory.getInstance().getAuthorizationStrategy(parameters.getActivity(), AuthorizationConfiguration.getInstance());

        Future<AuthorizationResult> future = strategy.requestAuthorization(getAuthorizationRequest(parameters), mAuthorizationStrategy);

        //We could implement Timeout Here if we wish instead of blocking indefinitely
        //future.get(10, TimeUnit.MINUTES);  // Need to handle timeout exception in the scenario it doesn't return within a reasonable amount of time
        AuthorizationResult result = future.get();

        return result;

    }

    private AuthorizationRequest getAuthorizationRequest(MSALAcquireTokenOperationParameters parameters){
        AuthorizationRequest authRequest = new MicrosoftStsAuthorizationRequest();

        String scopes = StringUtil.join(' ', parameters.getScopes());

        authRequest.setClientId(parameters.getClientId());
        authRequest.setRedirectUri(parameters.getRedirectUri());
        authRequest.setScope(scopes);
        authRequest.setResponseType(AuthorizationRequest.ResponseTypes.CODE);

        return authRequest;
    }

    private TokenResult performTokenRequest(OAuth2Strategy strategy, AuthorizationResponse response, MSALAcquireTokenOperationParameters parameters) {

        TokenRequest tr = new TokenRequest();

        tr.setCode(response.getCode());
        tr.setClientId(parameters.getClientId());
        tr.setRedirectUri(parameters.getRedirectUri());
        tr.setScope(StringUtil.join(' ', parameters.getScopes()));
        tr.setGrantType(TokenRequest.GrantTypes.AUTHORIZATION_CODE);

        TokenResult tokenResult = null;

        try {
            tokenResult = strategy.requestToken(tr);
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
    public void CompleteAcquireToken(int requestCode, int resultCode, final Intent data) {
        mAuthorizationStrategy.completeAuthorization(requestCode, resultCode, data);
    }

    @Override
    public AuthenticationResult AcquireTokenSilent(MSALAcquireTokenSilentOperationParameters request) {
        throw new UnsupportedOperationException();
    }
}
