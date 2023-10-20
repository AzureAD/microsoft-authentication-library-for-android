package com.microsoft.identity.client.msal.automationapp.testpass.broker.ltw;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;

import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.LTWTests;
import com.microsoft.identity.client.ui.automation.app.MsalTestApp;
import com.microsoft.identity.client.ui.automation.app.OneAuthTestApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Assert;
import org.junit.Test;

@LTWTests
public class TestCaseDummy90953 extends AbstractMsalBrokerTest {
    @Test
    public void test_dummy_90953() throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        // To make sure the device is in clean slate, uninstall mBroker here.
        mBroker.uninstall();
        final MsalTestApp msalTestApp = new MsalTestApp();
        msalTestApp.uninstall();


        // install legacy company portal
        final BrokerCompanyPortal brokerCompanyPortal = new BrokerCompanyPortal(BrokerCompanyPortal.OLD_COMPANY_PORTAL_APK,
                BrokerCompanyPortal.COMPANY_PORTAL_APK);
        brokerCompanyPortal.install();

        // install old OneAuthTestApp
        final OneAuthTestApp oldOneAuthTestApp = new OneAuthTestApp();
        oldOneAuthTestApp.installOldApk();
        oldOneAuthTestApp.launch();
        handleOneAuthTestAppFirstRunCorrectly(oldOneAuthTestApp);

        // acquire token interactively on OneAuthTestApp
        final FirstPartyAppPromptHandlerParameters promptHandlerParametersOneAuth = FirstPartyAppPromptHandlerParameters.builder()
                .broker(mBroker)
                .prompt(PromptParameter.LOGIN)
                .loginHint(username)
                .consentPageExpected(false)
                .speedBumpExpected(false)
                .sessionExpected(false)
                .expectingBrokerAccountChooserActivity(false)
                .expectingLoginPageAccountPicker(false)
                .enrollPageExpected(false)
                .build();
        oldOneAuthTestApp.addFirstAccount(username, password, promptHandlerParametersOneAuth);
        oldOneAuthTestApp.confirmAccount(username);
// NOT installing auth app
        // update Company Portal
        brokerCompanyPortal.update();

        // install new MsalTestApp
        msalTestApp.install();
        msalTestApp.launch();
        msalTestApp.handleFirstRun();
        // acquire token interactively on MsalTestApp and should not get prompt
        final MicrosoftStsPromptHandlerParameters promptHandlerParametersMsal = MicrosoftStsPromptHandlerParameters.builder()
                .prompt(PromptParameter.SELECT_ACCOUNT)
                .loginHint(username)
                .sessionExpected(false)
                .broker(mBroker)
                .expectingBrokerAccountChooserActivity(false)
                .expectingProvidedAccountInBroker(false)
                .expectingLoginPageAccountPicker(false)
                .expectingProvidedAccountInCookie(false)
                .consentPageExpected(false)
                .passwordPageExpected(false)
                .speedBumpExpected(false)
                .registerPageExpected(false)
                .enrollPageExpected(false)
                .staySignedInPageExpected(false)
                .verifyYourIdentityPageExpected(false)
                .howWouldYouLikeToSignInExpected(false)
                .build();

        msalTestApp.handleUserNameInput(username);

        Thread.sleep(5000);
        try {
            String tokenMsal = msalTestApp.acquireToken(username, password, promptHandlerParametersMsal, false);
            Assert.assertNotNull(tokenMsal);
        } catch (Exception e) {
            brokerCompanyPortal.createPowerLiftIncident();
        }
        // getPackageName on MsalTestApp and should be Company Portal
        msalTestApp.handleBackButton();
        final String activeBroker = msalTestApp.getActiveBrokerPackageName();
        Assert.assertEquals("Active broker pkg name : " + BrokerCompanyPortal.COMPANY_PORTAL_APP_PACKAGE_NAME, activeBroker);
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

    protected void handleOneAuthTestAppFirstRunCorrectly(OneAuthTestApp oneAuthTestApp) {
        CommonUtils.grantPackagePermission();
        oneAuthTestApp.handlePreferBrokerSwitchButton();
        try {
            oneAuthTestApp.selectFromAppConfiguration("com.microsoft.identity.LabsApi.Guest");
        } catch (UiObjectNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
