package com.microsoft.identity.client.msal.automationapp.testpass.broker.wpj;


import androidx.annotation.NonNull;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation;
import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.BrokerTestHelper;
import com.microsoft.identity.client.msal.automationapp.MsalLoggingRule;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
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

    protected ITestBroker mBroker = getBroker();
    protected BrokerHost mBrokerHost = getBrokerHost();

    @NonNull
    @Override
    public ITestBroker getBroker() {
        if (mBroker == null) {
            final IAppInstaller mBrokerSource = getBrokerSource();
            final SupportedBrokers supportedBrokersAnnotation = getClass().getAnnotation(SupportedBrokers.class);
            mBroker = BrokerTestHelper.createBrokerFlavorFromSource(supportedBrokersAnnotation, mBrokerSource);
        }
        return mBroker;
    }

    @NonNull
    @Override
    public BrokerHost getBrokerHost() {
        if (mBrokerHost == null) {
            final String brokerHostApkName = getBrokerHostApkName();
            if (null == brokerHostApkName){
                mBrokerHost = new BrokerHost();
            }else{
                mBrokerHost = new BrokerHost(brokerHostApkName);
            }
        }
        return mBrokerHost;
    }

    protected IAppInstaller getBrokerSource(){
        return BuildConfig.INSTALL_SOURCE_LOCAL_APK
                .equalsIgnoreCase(BuildConfig.BROKER_INSTALL_SOURCE)
                ? new LocalApkInstaller() : new PlayStore();
    }
    protected String getBrokerHostApkName(){
        return null;
    }

    public String getLabUserForCloudDeviceAdmin() {
        final LabUserQuery query = new LabUserQuery();
        query.userRole = LabConstants.UserRole.CLOUD_DEVICE_ADMINISTRATOR;
        return LabUserHelper.loadUserForTest(query);
    }

    public final MsalAuthTestParams getBasicAuthTestParams() {
        return MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(mLoginHint)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();
    }

    public final MsalAuthTestParams getTlsAuthTestParams() {
        return MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(mLoginHint)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .msalConfigResourceId(getTslConfigFileResourceId())
                .claims(getTslClaim())
                .build();
    }

    private int getTslConfigFileResourceId() {
        return R.raw.msal_config_instance_aware_common_skip_broker;
    }

    private ClaimsRequest getTslClaim() {
        // claim {"access_token":{"deviceid":{"essential":true}}}
        final ClaimsRequest claimsRequest = new ClaimsRequest();
        final RequestedClaimAdditionalInformation requestedClaimAdditionalInformation =
                new RequestedClaimAdditionalInformation();
        requestedClaimAdditionalInformation.setEssential(true);
        // request the deviceid claim in AT Token
        claimsRequest.requestClaimInAccessToken("deviceid", requestedClaimAdditionalInformation);
        return claimsRequest;
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
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
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



