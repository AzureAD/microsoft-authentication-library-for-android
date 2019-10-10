package com.microsoft.identity.client.robolectric.tests.network;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        AcquireTokenNetworkAADTest.class,
        AcquireTokenNetworkB2CTest.class
})
public class AcquireTokenNetworkTestSuite {
}
