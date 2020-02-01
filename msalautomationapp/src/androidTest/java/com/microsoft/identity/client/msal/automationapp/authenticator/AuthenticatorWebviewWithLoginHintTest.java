//package com.microsoft.identity.client.msal.automationapp.authenticator;
//
//import com.microsoft.identity.client.msal.automationapp.broker.BrokerAuthenticator;
//import com.microsoft.identity.client.msal.automationapp.utils.BrokerUtils;
//import com.microsoft.identity.client.msal.automationapp.utils.WebViewUtils;
//
//public class AuthenticatorWebviewWithLoginHintTest extends BrokerAuthenticator {
//    @Override
//    public void handleUserInteraction() {
//        if (BrokerUtils.isAuthenticatorOpen()) {
//            handleAccountPicker(mUsername);
//        } else {
//            WebViewUtils.handleWebViewWithLoginHint();
//        }
//    }
//}
