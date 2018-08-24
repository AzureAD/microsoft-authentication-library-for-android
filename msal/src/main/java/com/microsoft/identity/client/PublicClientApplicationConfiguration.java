package com.microsoft.identity.client;



import android.content.Context;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import com.microsoft.identity.client.authorities.Authority;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;

public class PublicClientApplicationConfiguration {

    @SerializedName("client_id")
    private String mClientId;

    @SerializedName("redirect_uri")
    private String mRedirectUri;

    @SerializedName("authorities")
    private List<Authority> mAuthorities;

    @SerializedName("authorization_user_agent")
    private String mAuthorizationAgentString;
    private AuthorizationAgent mAuthorizationAgent;

    @SerializedName("http")
    private HttpConfiguration mHttpConfiguration = new HttpConfiguration();


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
     * Gets the currently configured HTTP configuration for the public client application
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
        this.mAuthorizationAgent = AuthorizationAgent.valueOf(this.mAuthorizationAgentString.toUpperCase());
        return this.mAuthorizationAgent;
    }


}
