package com.microsoft.identity.client.msal.automationapp.testpass.broker;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Assert;
import org.junit.Test;

// Flight settings
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1561087
public class TestCase1561087  extends  AbstractMsalBrokerTest {
    @Test
    public void test_1561087() throws Throwable {
        // Set flights and get to check if the flight information is returned
        final String flightsJson =  "{\"SetFlightsTest\":\"true\"}";
        mBroker.setFlights(flightsJson);
        Assert.assertEquals(mBroker.getFlights(), flightsJson);

        // clear flights and get to check if the flights are cleared
        final String clearFlightsJson =  "{}";
        mBroker.setFlights(clearFlightsJson);
        Assert.assertEquals(mBroker.getFlights(), clearFlightsJson);
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
