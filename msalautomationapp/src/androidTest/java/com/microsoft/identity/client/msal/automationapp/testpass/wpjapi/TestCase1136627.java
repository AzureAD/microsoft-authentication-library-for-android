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

import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.AcquireTokenParameters;
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
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.internal.testutils.labutils.LabApiException;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabDeviceHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

// User-based join (non-shared) - Update API, Old Broker side.
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1136627
public class TestCase1136627 extends AbstractMsalBrokerTest {

    @Test
    public void test_1136627() throws MsalException, InterruptedException, UiObjectNotFoundException, LabApiException {
        final String username = mLoginHint;
        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

        // clearing Browsing history.
        BrowserChrome chrome = new BrowserChrome();
        chrome.clear();

        // installing BrokerHost.
        ITestBroker sBroker = new BrokerHost("BrokerHost_Prod.apk");
        sBroker.install();

        // perform DeviceRegistration.
        sBroker.performDeviceRegistration(username, password);

        //getting DeviceID.
        final String deviceID1 = sBroker.obtainDeviceId();

        final TokenRequestLatch latch = new TokenRequestLatch(1);

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(null)
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
                                .loginHint(null)
                                .sessionExpected(true)
                                .consentPageExpected(false)
                                .speedBumpExpected(false)
                                .broker(mBroker)
                                .expectingBrokerAccountChooserActivity(true)
                                .registerPageExpected(false)
                                .build();

                        new AadPromptHandler(promptHandlerParameters)
                                .handlePrompt(username, password);
                    }
                }
        );

        interactiveRequest.execute();
        latch.await(TokenRequestTimeout.LONG);

        //installing latest version of BrokerHost app.
        final ITestBroker tBroker = new BrokerHost("BrokerHost_RC.apk");
        tBroker.install();

        SupportingUtilities.getUpn(tBroker);

        // obtaining Deviceid.
        final String deviceID2 = tBroker.obtainDeviceId();

        Assert.assertEquals(deviceID1, deviceID2);

        // installing Certificate in the brokerHost.
        SupportingUtilities.installCertificate(tBroker);

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
        SupportingUtilities.performWpjLeave(tBroker);

        // Deleting device should fail.
        try {
            final boolean deviceDeleted = LabDeviceHelper.deleteDevice(username, deviceID1);
        } catch (LabApiException e) {
            Assert.assertTrue(e.getCode() == 400);
        }
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

