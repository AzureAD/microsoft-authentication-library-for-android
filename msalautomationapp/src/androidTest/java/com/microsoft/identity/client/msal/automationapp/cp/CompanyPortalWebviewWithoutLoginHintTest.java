//package com.microsoft.identity.client.msal.automationapp.cp;
//
//import com.microsoft.identity.client.msal.automationapp.broker.BrokerCompanyPortal;
//import com.microsoft.identity.client.msal.automationapp.utils.BrokerUtils;
//import com.microsoft.identity.client.msal.automationapp.utils.WebViewUtils;
//
//import org.junit.Before;
//
//public class CompanyPortalWebviewWithoutLoginHintTest extends BrokerCompanyPortal {
//
//    @Before
//    public void setup() {
//        super.setup();
//        mLoginHint = null;
//    }
//
//    @Override
//    public void handleUserInteraction() {
//        if (BrokerUtils.isCompanyPortalOpen()) {
//            BrokerUtils.handleAccountPicker(mUsername);
//        } else {
//            WebViewUtils.handleWebViewWithoutLoginHint(mUsername);
//        }
//    }
//}
