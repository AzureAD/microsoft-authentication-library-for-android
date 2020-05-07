package com.microsoft.identity.client.msal.automationapp.testpass.broker;

import com.microsoft.identity.client.msal.automationapp.AcquireTokenNetworkAbstractTest;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.broker.BrokerAuthenticator;
import com.microsoft.identity.client.msal.automationapp.broker.ITestBroker;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestCase833511 extends AcquireTokenNetworkAbstractTest {

    @Test
    public void test_833511() {
        mBroker.performSharedDeviceRegistration(
                mUsername, LabConfig.getCurrentLabConfig().getLabUserPassword()
        );
    }

    @Override
    public LabUserQuery getLabUserQuery() {
        final LabUserQuery query = new LabUserQuery();
        query.homeDomain = LabConstants.HomeDomain.LAB_3;
        return query;
    }

    @Override
    public String getTempUserType() {
        return null;
    }

    @Override
    public String[] getScopes() {
        return null;
    }

    @Override
    public String getAuthority() {
        return null;
    }

    @Override
    public ITestBroker getBroker() {
        return new BrokerAuthenticator();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_instance_aware_common;
    }

}
