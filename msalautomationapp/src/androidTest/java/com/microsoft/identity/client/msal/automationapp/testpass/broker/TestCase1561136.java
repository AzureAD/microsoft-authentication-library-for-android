package com.microsoft.identity.client.msal.automationapp.testpass.broker;


import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

@SupportedBrokers(brokers = BrokerHost.class)
public class TestCase1561136 extends AbstractMsalBrokerTest {

    @Test
    public void test_1561136() throws Throwable {
        final String username = mLoginHint;
        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();
        Assert.assertEquals(mBroker.getAccounts(), null);

        final MsalSdk msalSdk = new MsalSdk();

        final MsalAuthTestParams authTestParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(mLoginHint)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        final MsalAuthResult authResult = msalSdk.acquireTokenInteractive(authTestParams, new OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
                //  ((IApp) mBrowser).handleFirstRun();

                final MicrosoftStsPromptHandlerParameters promptHandlerParameters = MicrosoftStsPromptHandlerParameters.builder()
                        .prompt(PromptParameter.SELECT_ACCOUNT)
                        .loginHint(mLoginHint)
                        .sessionExpected(false)
                        .consentPageExpected(false)
                        //.speedBumpExpected(true)
                        .build();

                new MicrosoftStsPromptHandler(promptHandlerParameters)
                        .handlePrompt(username, password);
            }
        }, TokenRequestTimeout.MEDIUM);

        authResult.assertSuccess();
        Assert.assertNotEquals(mBroker.getAccounts(), null);

        //user-based join
        // create another temp user
        final String username2 = LabUserHelper.loadTempUser(getTempUserType());
        final String password2 = LabConfig.getCurrentLabConfig().getLabUserPassword();

        Assert.assertNotEquals(username, username2);
        mBroker.performDeviceRegistration(username2, password2);
        Assert.assertNotEquals(mBroker.getAccounts(), null);

        // Look for the dialog box
        final UiObject dialogBox = UiAutomatorUtils.obtainUiObjectWithResourceId(
                "android:id/message"
        );
        Assert.assertTrue(dialogBox.exists());
        try {
            if (dialogBox.getText() != null) {
                // get the textId if it is there, else return null (in case of error)
                final String[] dialogBoxText = dialogBox.getText().split(":");
                // look for the textId if present
                Assert.assertEquals(dialogBoxText[1],username+ " TenantID");
            }

        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        } finally {
            // dismiss dialog
            UiAutomatorUtils.handleButtonClick("android:id/button1");
        }
    }

    @Override
    public LabUserQuery getLabUserQuery() {
        final LabUserQuery query = new LabUserQuery();
        query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_CLOUD;
        return query;
    }

    @Override
    public String getTempUserType() {
        return LabConstants.TempUserType.BASIC;
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
