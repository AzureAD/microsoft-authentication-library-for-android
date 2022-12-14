package com.microsoft.identity.client.msal.automationapp.testpass.broker.cba;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Ignore;
import org.junit.Test;

// [Non-Joined] Sign in with Certificate https://docs.msidlab.com/accounts/cadfsv4.html
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1600553
@Ignore("CBA Automation not working on google pixel, ")
public class TestCase1600553 extends AbstractMsalBrokerCbaTest {

    @Test
    public void test_1600553() throws Throwable {
        final String username = "fIDLAB@msidlab4.com";
        getSettingsScreen().installCertFromDeviceDownloadFolder(certificateFileName(), "1234");
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

    // We expect this file to already be in the device's downloads page.
    @Override
    public String certificateFileName() {
        return "mfatest.pfx";
    }
}
