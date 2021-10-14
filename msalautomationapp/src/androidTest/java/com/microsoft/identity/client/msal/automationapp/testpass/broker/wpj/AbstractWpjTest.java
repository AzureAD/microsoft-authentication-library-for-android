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


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation;
import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.BrokerTestHelper;
import com.microsoft.identity.client.msal.automationapp.MsalLoggingRule;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.BuildConfig;
import com.microsoft.identity.client.ui.automation.IBrokerHostTest;
import com.microsoft.identity.client.ui.automation.IBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.installer.IAppInstaller;
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;
import com.microsoft.identity.client.ui.automation.installer.PlayStore;
import com.microsoft.identity.client.ui.automation.rules.DevicePinSetupRule;
import com.microsoft.identity.client.ui.automation.rules.RulesHelper;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Rule;
import org.junit.rules.RuleChain;

import java.util.Arrays;

/**
 * An MSAL test model that would leverage an {@link ITestBroker} and {@link IBrokerHostTest} installed on the device.
 */
public abstract class AbstractWpjTest extends AbstractMsalUiTest implements IBrokerTest, IBrokerHostTest {

    protected BrokerHost mBrokerHost = getBrokerHost();
    protected ITestBroker mBroker = getBroker();
    @Nullable
    protected abstract IAppInstaller brokerInstallationSource();
    @Nullable
    protected abstract String brokerHostApkName();

    @NonNull
    @Override
    public BrokerHost getBrokerHost() {
        // only initialize once....so calling getBrokerHost from anywhere returns the same instance
        if (mBrokerHost == null) {
            final String brokerHostApkName = brokerHostApkName();
            mBrokerHost = new BrokerHost(brokerHostApkName);
        }
        return mBrokerHost;
    }

    @NonNull
    @Override
    public ITestBroker getBroker() {
        // only initialize once....so calling getBroker from anywhere returns the same instance
        if (mBroker == null) {
            final IAppInstaller mBrokerSource = brokerInstallationSource();
            final SupportedBrokers supportedBrokersAnnotation = getClass().getAnnotation(SupportedBrokers.class);
            mBroker = BrokerTestHelper.createBrokerFromFlavor(supportedBrokersAnnotation, mBrokerSource);
        }
        return mBroker;
    }

    /**
     * Run LabUserQuery to get a cloud device administrator user
     *
     * @return cloud device administrator user
     */
    public String getLabUserForCloudDeviceAdmin() {
        final LabUserQuery query = new LabUserQuery();
        query.userRole = LabConstants.UserRole.CLOUD_DEVICE_ADMINISTRATOR;
        return LabUserHelper.loadUserForTest(query);
    }


    /**
     * Build MSAL acquire token parameters with default configuration for WPJ test cases
     */
    public final MsalAuthTestParams getBasicAuthTestParams() {
        return MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(mLoginHint)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();
    }

    /**
     * Build MSAL acquire token parameters required to perform Client TLS
     * https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1162698
     */
    public final MsalAuthTestParams getTlsAuthTestParams() {
        // request the deviceid claim in AT Token
        final ClaimsRequest claimsRequest = new ClaimsRequest();
        final RequestedClaimAdditionalInformation requestedClaimAdditionalInformation =
                new RequestedClaimAdditionalInformation();
        requestedClaimAdditionalInformation.setEssential(true);
        // claim {"access_token":{"deviceid":{"essential":true}}}
        claimsRequest.requestClaimInAccessToken("deviceid", requestedClaimAdditionalInformation);
        return MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(mLoginHint)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .msalConfigResourceId(R.raw.msal_config_instance_aware_common_skip_broker)
                .claims(claimsRequest)
                .build();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
    }

    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public LabUserQuery getLabUserQuery() { return null; }

    @Override
    public String getTempUserType() { return LabConstants.TempUserType.BASIC; }

    @Override
    public RuleChain getPrimaryRules() {
        return RulesHelper.getPrimaryRules(getBroker());
    }
}



