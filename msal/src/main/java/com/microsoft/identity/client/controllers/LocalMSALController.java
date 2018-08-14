package com.microsoft.identity.client.controllers;

import android.content.Intent;

import com.microsoft.identity.client.DeviceBrowserAuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class LocalMSALController extends MSALController{

    private OAuth2Strategy mOAuthStrategy = null;
    private AuthorizationStrategy mAuthorizationStrategy = null;

    @Override
    public void AcquireToken(MSALAcquireTokenRequest request) throws ExecutionException, InterruptedException {

        //TODO: Use factory to get applicable oAuth and Authorization strategies
        mOAuthStrategy = new MicrosoftStsOAuth2Strategy(new MicrosoftStsOAuth2Configuration());

        //TODO: Map MSAL Acquire Token Request to Authorization Request
        AuthorizationRequest authRequest = new MicrosoftStsAuthorizationRequest();

        authRequest.setActivity(request.getActivity());
        authRequest.setContext(request.getAppContext());
        authRequest.setClientId(request.getClientId());
        authRequest.setRedirectUri(request.getRedirectUri());
        authRequest.setScope(StringUtil.join(' ', request.getScopes()));


        //TODO: Replace with factory to create the correct Authorization Strategy based on device capabilities and configuration
        mAuthorizationStrategy = new DeviceBrowserAuthorizationStrategy();

        Future<AuthorizationResult> future = mOAuthStrategy.requestAuthorization(authRequest, mAuthorizationStrategy);

        future.get();

        //We could implement Timeout Here if we wish instead of looping forever
        //future.get(10, TimeUnit.MINUTES);  // Need to handle timeout exception in the scenario it doesn't return within a reasonable amount of time
        //AuthorizationResult authorizationResult = future.get();


    }

    @Override
    public void CompleteAcquireToken(int requestCode, int resultCode, final Intent data) {
        mAuthorizationStrategy.completeAuthorization(requestCode, resultCode, data);
    }

    @Override
    public void AcquireTokenSilent(MSALAcquireTokenSilentRequest request) {

    }
}
