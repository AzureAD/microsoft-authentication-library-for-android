package com.microsoft.identity.client.msal.automationapp.testpass.broker.ltw;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;

import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.LTWTests;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.annotations.RunOnAPI29Minus;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.app.MsalTestApp;
import com.microsoft.identity.client.ui.automation.app.OneAuthTestApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerLTW;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Assert;
import org.junit.Test;

// If LTW without broker is installed, updated MSAL should still get SSO
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2571508
@LTWTests
public class TestCase2571508  extends AbstractMsalBrokerTest {
    @Test
    public void test_2571508() throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        mBroker.uninstall();
        final BrokerMicrosoftAuthenticator brokerMicrosoftAuthenticator = new BrokerMicrosoftAuthenticator();
        brokerMicrosoftAuthenticator.install();
        brokerMicrosoftAuthenticator.launch();

        // Install old LTW
        final BrokerLTW brokerLTW = new BrokerLTW(BrokerLTW.OLD_BROKER_LTW_APK, BrokerLTW.BROKER_LTW_APK);
        brokerLTW.install();

//        // AcquireToken interactively on OneAuthTestApp
//        final OneAuthTestApp oneAuthTestApp = new OneAuthTestApp();
//        oneAuthTestApp.install();
//        oneAuthTestApp.launch();
//        oneAuthTestApp.handleFirstRun();
//
//        final FirstPartyAppPromptHandlerParameters promptHandlerParametersOneAuth = FirstPartyAppPromptHandlerParameters.builder()
//                .broker(mBroker)
//                .prompt(PromptParameter.LOGIN)
//                .loginHint(username)
//                .consentPageExpected(false)
//                .speedBumpExpected(false)
//                .sessionExpected(false)
//                .expectingBrokerAccountChooserActivity(false)
//                .expectingLoginPageAccountPicker(false)
//                .enrollPageExpected(false)
//                .build();
//        // Click on sign in button, prompted to enter username and password
//        oneAuthTestApp.addFirstAccount(username, password, promptHandlerParametersOneAuth);
//        oneAuthTestApp.confirmAccount(username);


        // Install new MSALTestApp
        final MsalTestApp msalTestApp = new MsalTestApp();
        msalTestApp.install();
        msalTestApp.launch();
        msalTestApp.handleFirstRun();

        final String activeBroker = msalTestApp.getActiveBrokerPackageName();
        Assert.assertEquals("Active broker pkg name : " + BrokerMicrosoftAuthenticator.AUTHENTICATOR_APP_PACKAGE_NAME, activeBroker);

//        final MicrosoftStsPromptHandlerParameters promptHandlerParametersMsal = MicrosoftStsPromptHandlerParameters.builder()
//                .prompt(PromptParameter.SELECT_ACCOUNT)
//                .loginHint(username)
//                .sessionExpected(false)
//                .broker(mBroker)
//                .expectingBrokerAccountChooserActivity(false)
//                .expectingProvidedAccountInBroker(false)
//                .expectingLoginPageAccountPicker(false)
//                .expectingProvidedAccountInCookie(false)
//                .consentPageExpected(false)
//                .passwordPageExpected(false)
//                .speedBumpExpected(false)
//                .registerPageExpected(false)
//                .enrollPageExpected(false)
//                .staySignedInPageExpected(false)
//                .verifyYourIdentityPageExpected(false)
//                .howWouldYouLikeToSignInExpected(false)
//                .build();

        // Add login hint as the username and Click on AcquireToken button
        // NOT prompted for credentials.
//        msalTestApp.handleUserNameInput(username);
//        final UiObject acquireTokenButton = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/btn_acquiretoken", 3000);
//        scrollToElement(acquireTokenButton);
//        acquireTokenButton.click();
//        final String token = msalTestApp.acquireToken(username, password, promptHandlerParametersMsal, false);
//        Assert.assertNotNull(token);


    }

    @Override
    public LabQuery getLabQuery() {
        return null;
    }

    @Override
    public TempUserType getTempUserType() {
        return TempUserType.BASIC;
    }

    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }

    private void scrollToElement(UiObject obj) throws UiObjectNotFoundException {
        UiScrollable scrollable = new UiScrollable(new UiSelector().scrollable(true));
        scrollable.scrollIntoView(obj);
        obj.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
    }
}