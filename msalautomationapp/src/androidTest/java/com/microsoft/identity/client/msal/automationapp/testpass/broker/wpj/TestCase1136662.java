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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.wpj;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.ui.automation.TestContext;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.app.AzureSampleApp;
import com.microsoft.identity.client.ui.automation.broker.AbstractTestBroker;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.installer.IAppInstaller;
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;
import com.microsoft.identity.client.ui.automation.installer.PlayStore;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.TlsPromptHandler;
import com.microsoft.identity.internal.testutils.labutils.LabApiException;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabDeviceHelper;
import com.microsoft.identity.labapi.utilities.jwt.IJWTParser;
import com.microsoft.identity.labapi.utilities.jwt.JWTParserFactory;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// [WPJ] User-based join (shared) - PROD API, Update Active Broker from PROD to RC
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1136662
@SupportedBrokers(brokers = {BrokerMicrosoftAuthenticator.class})
public class TestCase1136662 extends AbstractWpjTest{

    @Override
    protected IAppInstaller getBrokerSource(){
        return new PlayStore();
    }

    @Override
    protected String getBrokerHostApkName(){
        return BrokerHost.BROKER_HOST_APK_PROD;
    }

    @Test
    public void test_1136662() throws Throwable {

        final String username = mLoginHint;
        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();
        final String adminAccount = getLabUserForCloudDeviceAdmin();

        final MsalSdk msalSdk = new MsalSdk();
        final IJWTParser jwtParser = JWTParserFactory.INSTANCE.getJwtParser();

        // install BrokerHost
        mBrokerHost.install();
        // Perform user-based join Shared Device
        mBrokerHost.performSharedDeviceRegistration(adminAccount, password);
        // Get Device ID
        final String deviceId = mBrokerHost.obtainDeviceId();
        // Device is shared
        final boolean isDeviceSharedAfterDR =  mBrokerHost.isDeviceShared();
        Assert.assertTrue(isDeviceSharedAfterDR);
        // Start interactive token request in MSAL (User Should be prompted)
        final MsalAuthResult authResult = msalSdk.acquireTokenInteractive(getBasicAuthTestParams(), new OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
                final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                        .prompt(PromptParameter.SELECT_ACCOUNT)
                        .loginHint(mLoginHint)
                        .sessionExpected(true)
                        .consentPageExpected(false)
                        .speedBumpExpected(false)
                        .expectingBrokerAccountChooserActivity(false)
                        .passwordPageExpected(true)
                        .broker(mBroker)
                        .build();

                new AadPromptHandler(promptHandlerParameters)
                        .handlePrompt(username, password);
            }
        }, TokenRequestTimeout.MEDIUM);
        authResult.assertSuccess();
        // Check account in Azure sample
        AzureSampleApp azureSampleApp = new AzureSampleApp();
        azureSampleApp.install();
        azureSampleApp.launch();
        Thread.sleep(TimeUnit.SECONDS.toMillis(3));
        azureSampleApp.confirmSignedIn(username);
        // Update Active Broker from PROD to RC
        ((AbstractTestBroker)mBroker).setAppInstaller(new LocalApkInstaller());
        mBroker.install();
        // Validate upn, device [id, State, isShared]
        final String accountUpn = mBrokerHost.getAccountUpn();
        Assert.assertEquals(accountUpn, adminAccount);
        final String deviceIdAfterAcquireToken = mBrokerHost.obtainDeviceId();
        Assert.assertEquals(deviceIdAfterAcquireToken, deviceId);
        final String deviceState = mBrokerHost.getDeviceState();
        Assert.assertTrue("true".equalsIgnoreCase(deviceState));
        final boolean isDeviceShared =  mBrokerHost.isDeviceShared();
        Assert.assertTrue(isDeviceShared);
        // Install cert
        mBrokerHost.enableBrowserAccess();
        //Perform Client TLS, Acquire Token
        msalSdk.acquireTokenInteractive(getTlsAuthTestParams(), new OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
                final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                        .prompt(PromptParameter.SELECT_ACCOUNT)
                        .loginHint(mLoginHint)
                        .sessionExpected(false)
                        .expectingBrokerAccountChooserActivity(false)
                        .passwordPageExpected(true)
                        .broker(mBroker)
                        .build();

                new TlsPromptHandler(promptHandlerParameters)
                        .handlePrompt(username, password);

            }
        }, TokenRequestTimeout.MEDIUM);
        final MsalAuthResult authResultTls = msalSdk.acquireTokenInteractive(getBasicAuthTestParams(), new OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
            }
        }, TokenRequestTimeout.MEDIUM);
        authResultTls.assertSuccess();
        final String jwt = authResultTls.getAccessToken();
        final Map<String, ?> responseItems = jwtParser.parseJWT(jwt);
        final String jwtDeviceId = (String) responseItems.get("deviceid");
        Assert.assertEquals(deviceId, jwtDeviceId);
        // Forward time 1 hour to expire AT
        TestContext.getTestContext().getTestDevice().getSettings().forwardDeviceTimeForOneDay();
        // wpj leave
        final String wpjState = mBrokerHost.wpjLeave();
        Assert.assertEquals("Device leave successful.", wpjState);
        final String upnNull = mBrokerHost.getAccountUpn();
        Assert.assertNull("Device is not joined", upnNull);
        Thread.sleep(TimeUnit.SECONDS.toMillis(5));
        try {
            final boolean deviceDeleted = LabDeviceHelper.deleteDevice(accountUpn, deviceId);
        } catch (LabApiException e) {
            Assert.assertTrue(e.getCode() == 400);
        }
    }



}


