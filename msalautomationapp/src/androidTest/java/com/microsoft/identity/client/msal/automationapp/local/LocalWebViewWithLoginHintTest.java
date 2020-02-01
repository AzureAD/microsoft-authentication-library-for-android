package com.microsoft.identity.client.msal.automationapp.local;

import com.microsoft.identity.client.msal.automationapp.local.LocalMsalTest;
import com.microsoft.identity.client.msal.automationapp.utils.WebViewUtils;

import static org.junit.Assert.fail;

public class LocalWebViewWithLoginHintTest extends LocalMsalTest {

    @Override
    public void handleUserInteraction() {
        WebViewUtils.handleWebViewWithLoginHint();
    }
}
