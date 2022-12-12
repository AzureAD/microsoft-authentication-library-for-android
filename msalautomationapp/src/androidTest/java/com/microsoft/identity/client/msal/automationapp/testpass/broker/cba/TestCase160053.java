package com.microsoft.identity.client.msal.automationapp.testpass.broker.cba;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Ignore;
import org.junit.Test;

// [Non-Joined] Sign in with Certificate https://docs.msidlab.com/accounts/cadfsv4.html
// https://identitydivision.visualstudio.com/Engineering/_testPlans/define?planId=2007357&suiteId=2008835
@Ignore("CBA Automation not working yet")
public class TestCase160053 extends AbstractMsalBrokerTest {

    @Test
    public void test_160053() throws Throwable {
        final String username = "fIDLAB@msidlab4.com";
        final String password = mLabAccount.getPassword();

    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .azureEnvironment(AzureEnvironment.AZURE_CLOUD)
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
