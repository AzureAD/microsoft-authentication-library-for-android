package com.microsoft.identity.client.msal.automationapp.testpass.broker.ltw;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;

import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.LTWTests;
import com.microsoft.identity.client.ui.automation.annotations.RunOnAPI29Minus;
import com.microsoft.identity.client.ui.automation.app.MsalTestApp;
import com.microsoft.identity.client.ui.automation.app.OneAuthTestApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Test;

@LTWTests
@RunOnAPI29Minus
public class TestCaseDummy90952 extends AbstractMsalBrokerTest {
    @Test
    public void test_dummy_90952() throws Throwable {
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

        // install new Authenticator
        final BrokerMicrosoftAuthenticator brokerMicrosoftAuthenticator = new BrokerMicrosoftAuthenticator();
        brokerMicrosoftAuthenticator.install();
        brokerMicrosoftAuthenticator.launch();
        brokerMicrosoftAuthenticator.handleFirstRun();
        Thread.sleep(5000);

        //NOT  update Company Portal
       // brokerCompanyPortal.update();

        brokerCompanyPortal.createPowerLiftIncident();
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
