//package com.microsoft.identity.client.msal.automationapp.cp;
//
//import com.microsoft.identity.client.msal.automationapp.broker.BrokerCompanyPortal;
//import com.microsoft.identity.client.msal.automationapp.utils.BrokerUtils;
//import com.microsoft.identity.client.msal.automationapp.utils.WebViewUtils;
//
//public class CompanyPortalWebviewWithLoginHintTest extends BrokerCompanyPortal {
//
//    @Override
//    public void handleUserInteraction() {
//        if (BrokerUtils.isCompanyPortalOpen()) {
//            BrokerUtils.handleAccountPicker(mUsername);
//        } else {
//            WebViewUtils.handleWebViewWithLoginHint();
//        }
//    }
//}
