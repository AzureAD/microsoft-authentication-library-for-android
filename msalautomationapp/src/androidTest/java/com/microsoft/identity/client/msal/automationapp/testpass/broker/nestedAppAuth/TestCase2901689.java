package com.microsoft.identity.client.msal.automationapp.testpass.broker.nestedAppAuth;

import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractGuestAccountMsalBrokerUiTest;
import com.microsoft.identity.client.ui.automation.annotations.LocalBrokerHostDebugUiTest;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.GuestHomeAzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.GuestHomedIn;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Test;

// Test NAA silent request in cross cloud scenario
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2901689
@LocalBrokerHostDebugUiTest
@SupportedBrokers(brokers = {BrokerHost.class})
public class TestCase2901689 extends AbstractGuestAccountMsalBrokerUiTest {
    @Test
    public void test_2901689() {
        NestedAppHelper nestedAppHelper = new NestedAppHelper(mActivity, mGuestUser, getAuthority(), mLabClient);
        nestedAppHelper.performATForHubApp();
        // Interactive auth from outlook with same account.
        nestedAppHelper.performATForOutlookApp();

        try {
            // Test if ATS succeeds from nested app after outlook's interactive auth.
            nestedAppHelper.performATSilentForNestedApp(nestedAppHelper.getAccountRecordAfterHubAppAT(), false);
            // Test if above ATS call did not affect outlook app's ATS (ATS from outlook app should succeed).
            nestedAppHelper.performATSilentForOutlookApp(nestedAppHelper.getAccountRecordAfterHubAppAT());
            // ATS from hub app should succeed.
            nestedAppHelper.performATSilentForHubApp(nestedAppHelper.getAccountRecordAfterHubAppAT(), false);
        } catch (BaseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userType(UserType.GUEST)
                .guestHomeAzureEnvironment(GuestHomeAzureEnvironment.AZURE_US_GOVERNMENT)
                .guestHomedIn(GuestHomedIn.HOST_AZURE_AD)
                .azureEnvironment(AzureEnvironment.AZURE_CLOUD)
                .build();
    }

    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public String getAuthority() {
        return "https://login.microsoftonline.com/" + mGuestUser.getGuestLabTenants().get(0);
    }
}
