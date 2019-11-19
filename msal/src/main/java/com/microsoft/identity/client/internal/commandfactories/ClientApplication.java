package com.microsoft.identity.client.internal.commandfactories;

import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;

public class ClientApplication {
    private OAuth2TokenCache mTokenCache;
    private PublicClientApplicationConfiguration mConfiguration;

    public ClientApplication(OAuth2TokenCache cache, PublicClientApplicationConfiguration config){
        mTokenCache = cache;
        mConfiguration = config;
    }

    public OAuth2TokenCache getTokenCache() {
        return mTokenCache;
    }

    public PublicClientApplicationConfiguration getConfiguration(){
        return mConfiguration;
    }
}
