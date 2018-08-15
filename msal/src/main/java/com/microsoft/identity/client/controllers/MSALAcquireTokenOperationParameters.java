package com.microsoft.identity.client.controllers;

import android.app.Activity;
import android.content.Context;

import com.microsoft.identity.client.UiBehavior;
import com.microsoft.identity.client.User;

import java.util.List;

public class MSALAcquireTokenOperationParameters extends MSALOperationParameters {

    private Context mAppContext;
    private Activity mActivity;
    private List<String> mScopes;
    private User mUser;
    private UiBehavior mUIBehavior;
    private String mExtraQueryStringParameters;
    private String mExtraScopesToConsent;
    private String authority;
    private String clientId;
    private String redirectUri;


    public Context getAppContext() {
        return mAppContext;
    }

    public void setAppContext(Context mAppContext) {
        this.mAppContext = mAppContext;
    }

    public Activity getActivity() {
        return mActivity;
    }

    public void setActivity(Activity mActivity) {
        this.mActivity = mActivity;
    }

    public List<String> getScopes() {
        return mScopes;
    }

    public void setScopes(List<String> mScopes) {
        this.mScopes = mScopes;
    }

    public User getUser() {
        return mUser;
    }

    public void setUser(User mUser) {
        this.mUser = mUser;
    }

    public UiBehavior getUIBehavior() {
        return mUIBehavior;
    }

    public void setUIBehavior(UiBehavior mUIBehavior) {
        this.mUIBehavior = mUIBehavior;
    }

    public String getExtraQueryStringParameters() {
        return mExtraQueryStringParameters;
    }

    public void setExtraQueryStringParameters(String mExtraQueryStringParameters) {
        this.mExtraQueryStringParameters = mExtraQueryStringParameters;
    }

    public String getExtraScopesToConsent() {
        return mExtraScopesToConsent;
    }

    public void setExtraScopesToConsent(String mExtraScopesToConsent) {
        this.mExtraScopesToConsent = mExtraScopesToConsent;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}
