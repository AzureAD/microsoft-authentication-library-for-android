package com.microsoft.identity.client.robolectric.tests.network;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        AcquireTokenNetworkAADTest.class,
        AcquireTokenNetworkB2CTest.class
})
/**
 * This class runs all tests in the {@link AcquireTokenNetworkTest} class,
 * using both AAD and B2C
 */
public class AcquireTokenNetworkTestSuite {
}
