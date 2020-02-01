//package com.microsoft.identity.client.msal.automationapp.authenticator;
//
//import com.microsoft.identity.client.msal.automationapp.broker.BrokerAuthenticator;
//import com.microsoft.identity.client.msal.automationapp.utils.BrokerUtils;
//import com.microsoft.identity.client.msal.automationapp.utils.WebViewUtils;
//
//import org.junit.Before;
//
//public class AuthenticatorWebviewWithoutLoginHintTest extends BrokerAuthenticator {
//    @Before
//    public void setup() {
//        super.setup();
//        mLoginHint = null;
//    }
//
//    @Override
//    public void handleUserInteraction() {
//        if (BrokerUtils.isAuthenticatorOpen()) {
//            BrokerUtils.handleAccountPicker(mUsername);
//        } else {
//            WebViewUtils.handleWebViewWithoutLoginHint(mUsername);
//        }
//    }
//}
