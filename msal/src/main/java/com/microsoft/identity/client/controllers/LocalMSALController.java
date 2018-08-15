package com.microsoft.identity.client.controllers;

import android.content.Intent;

import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationConfiguration;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;

import com.microsoft.identity.common.internal.ui.AuthorizationStrategyFactory;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class LocalMSALController extends MSALController{


    private OAuth2Strategy mOAuthStrategy = null;
    private AuthorizationStrategy mAuthorizationStrategy = null;

    @Override
    public AuthenticationResult AcquireToken(MSALAcquireTokenOperationParameters request) throws ExecutionException, InterruptedException {


        //TODO: Use factory to get applicable oAuth and Authorization strategies
        mOAuthStrategy = new MicrosoftStsOAuth2Strategy(new MicrosoftStsOAuth2Configuration());

        //TODO: Map MSAL Acquire Token Request to Authorization Request
        AuthorizationRequest authRequest = new MicrosoftStsAuthorizationRequest();

        String scopes = StringUtil.join(' ', request.getScopes());

        authRequest.setClientId(request.getClientId());
        authRequest.setRedirectUri(request.getRedirectUri());
        authRequest.setScope(scopes);

        //TODO: Replace with factory to create the correct Authorization Strategy based on device capabilities and configuration
        mAuthorizationStrategy = AuthorizationStrategyFactory.getInstance().getAuthorizationStrategy(request.getActivity(), AuthorizationConfiguration.getInstance());

        Future<AuthorizationResult> future = mOAuthStrategy.requestAuthorization(authRequest, mAuthorizationStrategy);

        //Blocking Call
        AuthorizationResult result = future.get();

        TokenRequest tr = new TokenRequest();

        tr.setCode(result.getAuthorizationResponse().getCode());
        tr.setClientId(request.getClientId());
        tr.setRedirectUri(request.getRedirectUri());
        tr.setScope(scopes);
        tr.setGrantType(TokenRequest.GrantTypes.AUTHORIZATION_CODE);

        TokenResult tokenResult = null;

        try {
            tokenResult = mOAuthStrategy.requestToken(tr);
        } catch (IOException e) {
            //TODO: Figure out exception handling
        }

        if(tokenResult != null && tokenResult.getSuccess()){
            //TODO Create AuthenticationResult
        }

        //We could implement Timeout Here if we wish instead of looping forever
        //future.get(10, TimeUnit.MINUTES);  // Need to handle timeout exception in the scenario it doesn't return within a reasonable amount of time
        //AuthorizationResult authorizationResult = future.get();

        throw new UnsupportedOperationException();
    }

    @Override
    public void CompleteAcquireToken(int requestCode, int resultCode, final Intent data) {
        mAuthorizationStrategy.completeAuthorization(requestCode, resultCode, data);
    }

    @Override
    public AuthenticationResult AcquireTokenSilent(MSALAcquireTokenSilentOperationParameters request) {
        throw new UnsupportedOperationException();
    }
}
