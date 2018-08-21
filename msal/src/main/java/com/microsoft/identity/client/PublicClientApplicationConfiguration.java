package com.microsoft.identity.client;



import android.content.Context;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.identity.client.authorities.Authority;

public class PublicClientApplicationConfiguration {


    @SerializedName("clientid")
    private String mClientId;
    @SerializedName("redirect_uri")
    private String mRedirectUri;
    private transient Context mContext;
    @SerializedName("authorities")
    private List<Authority> mAuthorities;
    @SerializedName("http")
    private HttpConfiguration mHttpConfiguration = new HttpConfiguration();


    public String getClientId() {
        return mClientId;
    }

    public void setClientId(String clientId) {
        this.mClientId = clientId;
    }

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    public List<Authority> getAuthorities() {
        return mAuthorities;
    }

    public void setAuthorities(List<Authority> knownAuthorities) {
        this.mAuthorities = knownAuthorities;
    }

    public void setHttpConfiguration(HttpConfiguration config){
        this.mHttpConfiguration = config;
    }

    public HttpConfiguration getHttpConfiguration(){
        return this.mHttpConfiguration;
    }

    public String getRedirectUri(){
        return this.mRedirectUri;
    }

    public String setRedirectUri(String redirectUri){
        return this.mRedirectUri;
    }

}
