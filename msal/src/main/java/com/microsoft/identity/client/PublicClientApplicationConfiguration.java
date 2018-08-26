package com.microsoft.identity.client;



import com.google.gson.annotations.SerializedName;

import java.util.List;

import com.microsoft.identity.client.authorities.Authority;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;

public class PublicClientApplicationConfiguration {


    static final String CLIENT_ID_KEY = "client_id";
    static final String REDIRECT_URI_KEY = "redirect_uri";
    static final String AUTHORITIES_KEY = "authorities";
    static final String AUTHORIZATION_USER_AGENT_KEY = "authorization_user_agent";
    static final String HTTP_KEY = "http";

    @SerializedName(PublicClientApplicationConfiguration.CLIENT_ID_KEY)
    String mClientId;

    @SerializedName(PublicClientApplicationConfiguration.REDIRECT_URI_KEY)
    String mRedirectUri;

    @SerializedName(PublicClientApplicationConfiguration.AUTHORITIES_KEY)
    List<Authority> mAuthorities;

    @SerializedName(PublicClientApplicationConfiguration.AUTHORIZATION_USER_AGENT_KEY)
    AuthorizationAgent mAuthorizationAgent;

    @SerializedName(PublicClientApplicationConfiguration.HTTP_KEY)
    HttpConfiguration mHttpConfiguration = new HttpConfiguration();


    /**
     * Gets the currently configured client id for the public client application
     * @return
     */
    public String getClientId() {
        return mClientId;
    }

    /**
     * Gets the list of authorities configured by the developer for use with the public client application
     * @return
     */
    public List<Authority> getAuthorities() {
        return mAuthorities;
    }

    /**
     * Gets the currently configured HTTP_KEY configuration for the public client application
     * @return
     */
    public HttpConfiguration getHttpConfiguration(){
        return this.mHttpConfiguration;
    }

    /**
     * Gets the currently configured redirect uri for the public client application
     * @return
     */
    public String getRedirectUri(){
        return this.mRedirectUri;
    }

    /**
     * Gets the currently configured authorization agent for the public client application
     * @return
     */
    public AuthorizationAgent getAuthorizationAgent(){
        return this.mAuthorizationAgent;
    }


    void mergeConfiguration(PublicClientApplicationConfiguration config){


        this.mClientId = config.mClientId == null ? this.mClientId : config.mClientId;
        this.mRedirectUri = config.mRedirectUri == null ? this.mRedirectUri : config.mRedirectUri;
        this.mAuthorities = config.mAuthorities == null ? this.mAuthorities : config.mAuthorities;
        this.mAuthorizationAgent = config.mAuthorizationAgent == null ? this.mAuthorizationAgent : config.mAuthorizationAgent;
        this.mHttpConfiguration = config.mHttpConfiguration == null ? this.mHttpConfiguration : config.mHttpConfiguration;

    }

    void validateConfiguration(){
        nullConfigurationCheck(PublicClientApplicationConfiguration.REDIRECT_URI_KEY, mRedirectUri);
        nullConfigurationCheck(PublicClientApplicationConfiguration.CLIENT_ID_KEY, mClientId);
    }

    void nullConfigurationCheck(String configKey, String configValue){
        if(configValue == null){
            throw new IllegalArgumentException(configKey + " cannot be null.  Invalid configuration.");
        }
    }


}
