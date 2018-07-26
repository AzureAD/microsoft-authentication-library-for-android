package com.microsoft.identity.client.controllers;

import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;

public class LocalMSALController extends MSALController{
    @Override
    public void AcquireToken(MSALAcquireTokenRequest request) {
        //TODO: Use factory to get applicable oAuth and Authorization strategies
        OAuth2Strategy strategy = new MicrosoftStsOAuth2Strategy(new MicrosoftStsOAuth2Configuration());
        AuthorizationStrategy authorizationStrategy = new Au
        strategy.requestAuthorization(Authorizait)
    }

    @Override
    public void AcquireTokenSilent() {

    }
}
