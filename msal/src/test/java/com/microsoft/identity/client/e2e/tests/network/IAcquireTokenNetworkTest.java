package com.microsoft.identity.client.e2e.tests.network;

import com.microsoft.identity.client.e2e.tests.IAcquireTokenTest;
import com.microsoft.identity.internal.testutils.labutils.TestConfigurationQuery;

public interface IAcquireTokenNetworkTest extends IAcquireTokenTest {

    TestConfigurationQuery getTestConfigurationQuery();

}


