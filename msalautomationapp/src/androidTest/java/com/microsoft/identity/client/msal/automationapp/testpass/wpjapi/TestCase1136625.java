package com.microsoft.identity.client.msal.automationapp.testpass.wpjapi;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.MultipleAccountPublicClientApplication;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.interaction.InteractiveRequest;
import com.microsoft.identity.client.msal.automationapp.interaction.OnInteractionRequired;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TestContext;
import com.microsoft.identity.client.ui.automation.TokenRequestLatch;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;
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
import java.util.concurrent.CountDownLatch;

public class TestCase1136625 extends AbstractMsalBrokerTest {

    @Test
    public void test_1136625() throws InterruptedException, UiObjectNotFoundException, MsalException {
        final String username = mLoginHint;
        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

        // clearing Browsing history.
        BrowserChrome chrome = new BrowserChrome();
        chrome.clear();

        // installing BrokerHost.
        ITestBroker sBroker = new BrokerHost();
        sBroker.install();

        // perform DeviceRegistration.
        sBroker.performDeviceRegistration(username, password);

        //getting DeviceID.
        final String deviceID1 = sBroker.obtainDeviceId();

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
                                .expectingBrokerAccountChooserActivity(false)
                                .registerPageExpected(false)
                                .build();

                        new AadPromptHandler(promptHandlerParameters)
                                .handlePrompt(username, password);
                    }
                }
        );

        interactiveRequest.execute();
        latch.await(TokenRequestTimeout.LONG);

        //installing latest version of Microsoft authenticator app.
        final ITestBroker localBrokerAuthenticator = new BrokerMicrosoftAuthenticator(new LocalApkInstaller());
        localBrokerAuthenticator.install();

       SupportingUtilities.getUpn(sBroker);

        // obtaining Deviceid.
        final String deviceID2 = sBroker.obtainDeviceId();

        Assert.assertEquals(deviceID1, deviceID2);

        // installing Certificate in the brokerHost.
        SupportingUtilities.installCertificate(sBroker);

        mApplication = PublicClientApplication.create(mContext, R.raw.msal_config_instance_aware_common_skip_broker);

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
                        interactiveLatch, "deviceid", deviceID1
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

        SupportingUtilities.performTLSOperation(username, password);
        interactiveLatch.await(TokenRequestTimeout.LONG);

        // advance clock by more than an hour to expire AT in cache
        TestContext.getTestContext().getTestDevice().getSettings().forwardDeviceTimeForOneDay();

        // performing wpj leave.
        SupportingUtilities.performWpjLeave(sBroker);

        // Deleting device.
//        LabDeviceHelper.deleteDevice(username, deviceID1);
//
//        // asserting msal automation app in multiple account mode.
//        mApplication = PublicClientApplication.create(mContext, getConfigFileResourceId());
//        Assert.assertTrue(mApplication instanceof MultipleAccountPublicClientApplication);

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
        return null;
    }

    @Override
    public String getTempUserType() {
        return LabConstants.TempUserType.BASIC;
    }
}
