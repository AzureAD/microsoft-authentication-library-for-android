package com.microsoft.identity.client.msal.automationapp.testpass.broker.ltw;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.browser.IBrowser;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Assert;
import org.junit.Test;

@LTWTests
@RunOnAPI29Minus
public class TestDummy90955 extends AbstractMsalBrokerTest {

    @Test
    public void test_dummy_9095_5() throws Throwable {

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

        // update Company Portal
        brokerCompanyPortal.update();

        // install new MsalTestApp
        msalTestApp.install();
        msalTestApp.launch();
        msalTestApp.handleFirstRun();

        // This one takes company portal instead of broker!
        // acquire token interactively on MsalTestApp and should not get prompt
        final MicrosoftStsPromptHandlerParameters promptHandlerParametersMsal = MicrosoftStsPromptHandlerParameters.builder()
                .prompt(PromptParameter.SELECT_ACCOUNT)
                .loginHint(username)
                .sessionExpected(false)
                .broker(brokerCompanyPortal)
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
        String tokenMsal = acquireToken(username, password, promptHandlerParametersMsal);
        Assert.assertNotNull(tokenMsal);

        // getPackageName on MsalTestApp and should be Company Portal
        msalTestApp.handleBackButton();
        final String activeBroker = msalTestApp.getActiveBrokerPackageName();
        Assert.assertEquals("Active broker pkg name : " + BrokerCompanyPortal.COMPANY_PORTAL_APP_PACKAGE_NAME, activeBroker);
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

    @Override
    public LabQuery getLabQuery() {
        return null;
    }
    private void scrollToElement(UiObject obj) throws UiObjectNotFoundException {
        UiScrollable scrollable = new UiScrollable(new UiSelector().scrollable(true));
        scrollable.scrollIntoView(obj);
        obj.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
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

    private String acquireToken(@NonNull final String username,
                               @NonNull final String password,
                               @NonNull final PromptHandlerParameters promptHandlerParameters) throws UiObjectNotFoundException, InterruptedException {

        final UiObject acquireTokenButton = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/btn_acquiretoken", 5000);
        scrollToElement(acquireTokenButton);
        acquireTokenButton.click();


//            try {
//                final UiObject emailField = UiAutomatorUtils.obtainUiObjectWithTextAndClassType(
//                        "", EditText.class);
//                emailField.setText(username);
//                final UiObject nextBtn = UiAutomatorUtils.obtainUiObjectWithTextAndClassType(
//                        "Next", Button.class);
//                nextBtn.click();
//            } catch (final UiObjectNotFoundException e) {
//                throw new AssertionError("Could not click on object with txt Next");
//            }
//            final MicrosoftStsPromptHandler microsoftStsPromptHandler = new MicrosoftStsPromptHandler((MicrosoftStsPromptHandlerParameters) promptHandlerParameters);
//            microsoftStsPromptHandler.handlePrompt(username, password);


        // get token and return
        final UiObject result = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/txt_result", 5000);
        return result.getText();
    }
}
