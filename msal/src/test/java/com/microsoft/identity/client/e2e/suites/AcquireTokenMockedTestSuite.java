package com.microsoft.identity.client.e2e.suites;

import com.microsoft.identity.client.e2e.tests.mocked.CommandResultCachingTest;
import com.microsoft.identity.client.e2e.tests.mocked.MultipleAccountAcquireTokenMockTest;
import com.microsoft.identity.client.e2e.tests.mocked.SingleAccountAcquireTokenMockTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        CommandResultCachingTest.class,
        SingleAccountAcquireTokenMockTest.class,
        MultipleAccountAcquireTokenMockTest.class
})

public class AcquireTokenMockedTestSuite {
}
