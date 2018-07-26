package com.microsoft.identity.client.controllers;

import android.app.Activity;

import com.microsoft.identity.client.UiBehavior;
import com.microsoft.identity.client.User;

import java.util.List;

public class MSALAcquireTokenRequest {

    private Activity mActivity;
    private List<String> mScopes;
    private User mUser;
    private UiBehavior mUIBehavior;
    private String mExtraQueryStringParameters;
    private String mExtraScopesToConsent;
    private String authority;


}
