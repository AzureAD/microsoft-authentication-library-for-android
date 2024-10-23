package com.microsoft.identity.client.msal.automationapp.testpass.broker.ltw;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.msal.automationapp.BuildConfig;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.LTWTests;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.app.MsalTestApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerLTW;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

// Sign in with AAD and MSA account
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/3029738
@LTWTests
@RetryOnFailure
@SupportedBrokers(brokers = {BrokerLTW.class})
@RunWith(Parameterized.class)
public class TestCase3029738 extends AbstractMsalBrokerTest {

    private final UserType mUserType;

    public TestCase3029738(@NonNull UserType userType) {
        mUserType = userType;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<UserType> userType() {
        return Arrays.asList(
                UserType.MSA,
                UserType.CLOUD
        );
    }

    @Test
    public void test() throws Throwable {
        // Check flight, this is checking what was passed to automation app, not the broker apks
        Assume.assumeTrue( "EnableSystemAccountManager flight is not activated, Test will be skipped",
                BuildConfig.COPY_OF_LOCAL_FLIGHTS_FOR_TEST_PURPOSES.contains("EnableSystemAccountManager:true"));

        // Fetch account credentials
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        // Install and launch msal test app
        // set configuration based on user type
        final MsalTestApp msalTestApp = new MsalTestApp();
        msalTestApp.install();
        msalTestApp.launch();
        msalTestApp.handleFirstRunBasedOnUserType(mUserType);

        // Prompt handler for the subsequent
        final MicrosoftStsPromptHandlerParameters promptHandlerParameters = MicrosoftStsPromptHandlerParameters.builder()
                .prompt(PromptParameter.SELECT_ACCOUNT)
                .loginHint(username)
                .sessionExpected(false)
                .broker(mBroker)
                .expectingBrokerAccountChooserActivity(false)
                .expectingProvidedAccountInBroker(false)
                .expectingLoginPageAccountPicker(false)
                .expectingProvidedAccountInCookie(false)
                .consentPageExpected(false)
                .passwordPageExpected(true)
                .speedBumpExpected(false)
                .registerPageExpected(false)
                .enrollPageExpected(false)
                .staySignedInPageExpected(false)
                .verifyYourIdentityPageExpected(false)
                .howWouldYouLikeToSignInExpected(false)
                .build();

        // Make sure we get a token
        final String token = msalTestApp.acquireToken(username, password, promptHandlerParameters, true);
        Assert.assertNotNull(token);

        // Launch OS Account page and make sure username shows up in account manager.
        getSettingsScreen().launchAccountListPage();
        Assert.assertTrue(UiAutomatorUtils.obtainUiObjectWithText(username).exists());
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userType(mUserType)
                .build();
    }

    @Override
    public TempUserType getTempUserType() {
        return null;
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
}
