package com.microsoft.identity.client.msal.automationapp;

import com.microsoft.identity.client.msal.automationapp.utils.WebViewUtils;

public class WebViewWithLoginHint extends AcquireTokenNetworkTest {

    @Override
    public void handleUserInteraction() {
        if (mBroker != null && mBroker.isBrokerOpen()) {
            mBroker.handleAccountPicker(mUsername);
        } else {
            WebViewUtils.handleWebViewWithLoginHint();
        }
    }

}
