package com.microsoft.identity.client.msal.automationapp;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.msal.automationapp.broker.BrokerAuthenticator;
import com.microsoft.identity.client.msal.automationapp.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.msal.automationapp.broker.ITestBroker;
import com.microsoft.identity.client.msal.automationapp.utils.PlayStoreUtils;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public abstract class AcquireTokenNetworkAbstractTest extends AcquireTokenAbstractTest implements IAcquireTokenNetworkTest {

    protected String mUsername;
    protected String mLoginHint;

    @Parameterized.Parameter(0)
    public String mCloudName;

    @Parameterized.Parameter(1)
    public ITestBroker mBroker; // null or Authenticator or CP

    @Parameterized.Parameter(2)
    public String mBrokerName;

//    @Parameterized.Parameter(1)
//    public LabUserQuery mQuery;

    // creates the test data
    @Parameterized.Parameters(name = "{index}: Cloud={0} Broker={2}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {LabConstants.AzureEnvironment.AZURE_CLOUD, null, "NONE"},
                {LabConstants.AzureEnvironment.AZURE_CLOUD, new BrokerAuthenticator(), "Authenticator"},
                {LabConstants.AzureEnvironment.AZURE_CLOUD, new BrokerCompanyPortal(), "Company Portal"}
        };
        return Arrays.asList(data);
    }

    @Before
    public void setup() {
        System.setProperty(
                "org.mockito.android.target",
                ApplicationProvider
                        .getApplicationContext()
                        .getCacheDir()
                        .getPath()
        );

        AcquireTokenTestHelper.setAccount(null);
        final LabUserQuery query = new LabUserQuery(); //getLabUserQuery();
        query.azureEnvironment = mCloudName;
        mLoginHint = mUsername = LabUserHelper.loadUserForTest(query);
        super.setup();
        if (mBroker != null) {
            PlayStoreUtils.installApp(mBroker.brokerAppName());
        }
    }
}
