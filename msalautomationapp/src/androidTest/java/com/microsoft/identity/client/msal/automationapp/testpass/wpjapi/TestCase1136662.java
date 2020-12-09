//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
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
import com.microsoft.identity.client.ui.automation.TestContext;
import com.microsoft.identity.client.ui.automation.TokenRequestLatch;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.app.AzureSampleApp;
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
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Assert;
import org.junit.Test;

import java.security.Permission;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// User-based join (shared) - Old API, Update Broker side.
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1136662
public class TestCase1136662 extends AbstractMsalBrokerTest {

    @Test
    public void test_1136662() throws InterruptedException, MsalException, UiObjectNotFoundException {

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

        // creating temp user of type Basic.
        final String username2 = LabUserHelper.loadTempUser(LabConstants.TempUserType.BASIC);
        final String password2 = LabConfig.getCurrentLabConfig().getLabUserPassword();

        // re-create PCA after device registration
        mApplication = PublicClientApplication.create(mContext, getConfigFileResourceId());

        // pca should now be in SINGLE account mode
        Assert.assertTrue(mApplication instanceof SingleAccountPublicClientApplication);

        // acquiring token with username2.
        final CountDownLatch latch = new CountDownLatch(1);

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(username2)
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
                                .loginHint(username2)
                                .sessionExpected(false)
                                .consentPageExpected(false)
                                .speedBumpExpected(false)
                                .broker(mBroker)
                                .expectingBrokerAccountChooserActivity(false)
                                .registerPageExpected(false)
                                .build();

                        new AadPromptHandler(promptHandlerParameters)
                                .handlePrompt(username2, password2);
                    }
                }
        );

        interactiveRequest.execute();
        latch.await();

        // installing Azure Sample App.
        final AzureSampleApp azureSampleApp = new AzureSampleApp();
        azureSampleApp.uninstall();
        azureSampleApp.install();
        azureSampleApp.launch();
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));
        azureSampleApp.confirmSignedIn(username2);

        // installing Microsoft authenticator app using local apk.
        final ITestBroker localBrokerAuthenticator = new BrokerMicrosoftAuthenticator(new LocalApkInstaller());
        localBrokerAuthenticator.install();

        // obtaining device id.
        final String deviceID2 = sBroker.obtainDeviceId();
        Assert.assertEquals(deviceID2, deviceID1);

        // obtaining wpj upn.
        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.testuserapp:id/buttonGetWpjUpn");

        // Look for the UPN dialog box
        final UiObject showUpnDialog = UiAutomatorUtils.obtainUiObjectWithResourceId(
                "android:id/message"
        );

        Assert.assertTrue(showUpnDialog.exists());

        final String upn = showUpnDialog.getText().split(":")[1];

        // dismiss dialog
        UiAutomatorUtils.handleButtonClick("android:id/button1");

        Assert.assertEquals(upn, username);

        //installing certificate.
        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.testuserapp:id/buttonInstallCert");
        UiAutomatorUtils.handleButtonClick("android:id/button1");

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
        tlsOperation.performTLSOperation(username2, password2);

        interactiveLatch.await(TokenRequestTimeout.LONG);

        // advance clock by more than an hour to expire AT in cache
        TestContext.getTestContext().getTestDevice().getSettings().forwardDeviceTimeForOneDay();

        sBroker.launch();
        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.testuserapp:id/buttonLeave");
        Thread.sleep(TimeUnit.SECONDS.toMillis(30));

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
