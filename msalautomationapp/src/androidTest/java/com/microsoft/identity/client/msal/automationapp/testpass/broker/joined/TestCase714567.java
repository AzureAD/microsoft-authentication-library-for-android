package com.microsoft.identity.client.msal.automationapp.testpass.broker.joined;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserRole;

import org.junit.Test;

// [Broker] Device registration via Settings page (with Authenticator as broker)
// https://identitydivision.visualstudio.com/Engineering/_testPlans/define?planId=2007357&suiteId=2008868
@SupportedBrokers(brokers = BrokerMicrosoftAuthenticator.class)
public class TestCase714567 extends AbstractMsalBrokerTest {

    @Test
    public void test_714567() {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        ((BrokerMicrosoftAuthenticator) mBroker).shouldUseDeviceSettingsPage(false);
        mBroker.performDeviceRegistration(username, password);
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userRole(UserRole.CLOUD_DEVICE_ADMINISTRATOR)
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
