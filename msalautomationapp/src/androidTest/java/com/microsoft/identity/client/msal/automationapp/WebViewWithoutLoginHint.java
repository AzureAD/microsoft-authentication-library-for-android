package com.microsoft.identity.client.msal.automationapp;

import com.microsoft.identity.client.msal.automationapp.utils.WebViewUtils;

import org.junit.Before;

public class WebViewWithoutLoginHint extends AcquireTokenNetworkTest {
    @Before
    public void setup() {
        super.setup();
        mLoginHint = null;
    }

    @Override
    public void handleUserInteraction() {
        if (mBroker != null && mBroker.isBrokerOpen()) {
            mBroker.handleAccountPicker(mUsername);
        } else {
            WebViewUtils.handleWebViewWithoutLoginHint(mUsername);
        }
    }
}
