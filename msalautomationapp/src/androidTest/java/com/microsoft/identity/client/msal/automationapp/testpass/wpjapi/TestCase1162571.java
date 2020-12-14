package com.microsoft.identity.client.msal.automationapp.testpass.wpjapi;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SingleAccountPublicClientApplication;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.interaction.InteractiveRequest;
import com.microsoft.identity.client.msal.automationapp.interaction.OnInteractionRequired;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TokenRequestLatch;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.app.AzureSampleApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabDeviceHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class TestCase1162571 extends AbstractMsalBrokerTest {

    @Test
    public void test_1162571() throws MsalException, InterruptedException, UiObjectNotFoundException {

        final String username = mLoginHint;
        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

        //clearing Browsing History
        final BrowserChrome chrome = new BrowserChrome();
        chrome.clear();

        // installing Broker Host Broker.
        final ITestBroker sBroker = new BrokerHost();
        sBroker.install();

        // performing shared device registration.
        sBroker.performSharedDeviceRegistration(username, password);

        // getting DeviceID.
        final String deviceID1 = sBroker.obtainDeviceId();

        //installing certificate.
        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.testuserapp:id/buttonInstallCert");
        UiAutomatorUtils.handleButtonClick("android:id/button1");

        mApplication = PublicClientApplication.create(mContext, getConfigFileResourceId());

        // pca should now be in SINGLE account mode
        Assert.assertTrue(mApplication instanceof SingleAccountPublicClientApplication);

        mApplication = PublicClientApplication.create(mContext, R.raw.msal_config_instance_aware_common_skip_broker);

        // calling acquire token.
        final TokenRequestLatch interactiveLatch = new TokenRequestLatch(1);

        // create claims request object
        final ClaimsRequest claimsRequest = new ClaimsRequest();
        final RequestedClaimAdditionalInformation requestedClaimAdditionalInformation =
                new RequestedClaimAdditionalInformation();

        requestedClaimAdditionalInformation.setEssential(true);

        // request the deviceid claim in ID Token
        claimsRequest.requestClaimInIdToken("deviceid", requestedClaimAdditionalInformation);

        final AcquireTokenParameters newParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulClaimsRequestInIdTokenInteractiveCallback(
                        interactiveLatch, "deviceid", null
                ))
                .withPrompt(Prompt.LOGIN)
                .withClaims(claimsRequest)
                .withLoginHint(null)
                .build();

        final InteractiveRequest newInteractiveRequest = new InteractiveRequest(
                mApplication,
                newParameters,
                new OnInteractionRequired() {
                    @Override
                    public void handleUserInteraction() {
                        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                                .prompt(PromptParameter.LOGIN)
                                .loginHint(null)
                                .sessionExpected(false)
                                .consentPageExpected(false)
                                .speedBumpExpected(false)
                                .broker(mBroker)
                                .expectingBrokerAccountChooserActivity(false)
                                .expectingLoginPageAccountPicker(false)
                                .registerPageExpected(false)
                                .build();

                        new AadPromptHandler(promptHandlerParameters);
                    }
                }
        );

        newInteractiveRequest.execute();

        Tls tlsOperation = new Tls();
        tlsOperation.performTLSOperation(username, password);

        interactiveLatch.await();

        final TokenRequestLatch latch = new TokenRequestLatch(1);

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(username)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback(latch))
                .withPrompt(Prompt.SELECT_ACCOUNT)
                .build();

        final InteractiveRequest interactiveRequest = new InteractiveRequest(
                mApplication,
                parameters,
                new OnInteractionRequired() {
                    @Override
                    public void handleUserInteraction() {
                        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                                .prompt(PromptParameter.SELECT_ACCOUNT)
                                .loginHint(username)
                                .sessionExpected(false)
                                .consentPageExpected(false)
                                .speedBumpExpected(false)
                                .broker(mBroker)
                                .expectingBrokerAccountChooserActivity(true)
                                .registerPageExpected(false)
                                .build();

                        new AadPromptHandler(promptHandlerParameters);
                    }
                }
        );

        interactiveRequest.execute();
        UiObject continueButton = UiAutomatorUtils.obtainUiObjectWithExactText("Continue");
        continueButton.click();
        latch.await();

        // installing Azure Sample App.
        final AzureSampleApp azureSampleApp = new AzureSampleApp();
        azureSampleApp.uninstall();
        azureSampleApp.install();
        azureSampleApp.launch();
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));
        azureSampleApp.confirmSignedIn(username);

        sBroker.launch();
        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.testuserapp:id/buttonLeave");
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));

        // getting wpj upn which should be error.
        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.testuserapp:id/buttonGetWpjUpn");

        // Look for the UPN dialog box
        final UiObject showUpnDialogBox = UiAutomatorUtils.obtainUiObjectWithResourceId(
                "android:id/message"
        );

        Assert.assertTrue(showUpnDialogBox.exists());

        final String newUpn = showUpnDialogBox.getText().split(":")[0];

        // dismiss dialog
        UiAutomatorUtils.handleButtonClick("android:id/button1");
        Assert.assertEquals(newUpn, "Error");

        boolean deleteDevice = LabDeviceHelper.deleteDevice(username, deviceID1);
        Assert.assertEquals(deleteDevice, false);

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

    @Override
    public LabUserQuery getLabUserQuery() {
        final LabUserQuery query = new LabUserQuery();
        query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_CLOUD;
        query.userRole = LabConstants.UserRole.CLOUD_DEVICE_ADMINISTRATOR;
        return query;
    }

    @Override
    public String getTempUserType() {
        return null;
    }
}
