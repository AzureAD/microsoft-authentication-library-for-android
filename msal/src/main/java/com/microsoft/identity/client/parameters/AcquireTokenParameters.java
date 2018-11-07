package com.microsoft.identity.client.parameters;

import android.app.Activity;
import android.util.Pair;

import com.microsoft.identity.client.UiBehavior;

import java.util.List;

public class AcquireTokenParameters extends TokenParameters {

    private Activity mActivity;
    private String mLoginHint;
    private UiBehavior mUIBehavior;
    private List<String> mExtraScopesToConsent;
    private List<Pair<String, String>> mExtraQueryStringParameters;


    public Activity getActivity() {
        return mActivity;
    }

    public void setActivity(Activity activity) {
        this.mActivity = activity;
    }

    public String getLoginHint() {
        return mLoginHint;
    }

    public void setLoginHint(String loginHint) {
        this.mLoginHint = loginHint;
    }

    public UiBehavior getUIBehavior() {
        return mUIBehavior;
    }

    public void setUIBehavior(UiBehavior uiBehavior) {
        this.mUIBehavior = uiBehavior;
    }

    public List<String> getExtraScopesToConsent() {
        return mExtraScopesToConsent;
    }

    public void setExtraScopesToConsent(List<String> extraScopesToConsent) {
        this.mExtraScopesToConsent = extraScopesToConsent;
    }

    public List<Pair<String, String>> getExtraQueryStringParameters() {
        return mExtraQueryStringParameters;
    }

    public void setExtraQueryStringParameters(List<Pair<String, String>> extraQueryStringParameters) {
        this.mExtraQueryStringParameters = extraQueryStringParameters;
    }

}
