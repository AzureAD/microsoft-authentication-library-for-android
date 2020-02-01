package com.microsoft.identity.client.msal.automationapp.local;

import com.microsoft.identity.client.msal.automationapp.AcquireTokenNetworkTest;
import com.microsoft.identity.client.msal.automationapp.ErrorCodes;
import com.microsoft.identity.internal.testutils.TestUtils;

import org.junit.Before;
import org.junit.Test;

public abstract class LocalMsalTest extends AcquireTokenNetworkTest {

    @Before
    public void setup() {
        super.setup();
    }

    @Test
    public void testAcquireTokenSilentFailureEmptyCache() throws InterruptedException {
        performAcquireTokenInteractive();

        // clear the cache now
        TestUtils.clearCache(SHARED_PREFERENCES_NAME);

        performAcquireTokenSilent(false, ErrorCodes.NO_ACCOUNT_FOUND_ERROR_CODE);
    }

    @Test
    public void testAcquireTokenSilentSuccessCacheWithNoAccessToken() throws InterruptedException {
        performAcquireTokenInteractive();
        // remove the access token from cache
        TestUtils.removeAccessTokenFromCache(SHARED_PREFERENCES_NAME);

        performAcquireTokenSilent(false);
    }

}
