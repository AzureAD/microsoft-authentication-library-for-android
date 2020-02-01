package com.microsoft.identity.client.msal.automationapp.local;

import com.microsoft.identity.client.msal.automationapp.local.LocalMsalTest;
import com.microsoft.identity.client.msal.automationapp.utils.WebViewUtils;

import org.junit.Before;

public class LocalWebViewWithoutLoginHintTest extends LocalMsalTest {

    @Before
    public void setup() {
        super.setup();
        mLoginHint = null;
    }

    @Override
    public void handleUserInteraction() {
        WebViewUtils.handleWebViewWithoutLoginHint(mUsername);
    }
}
