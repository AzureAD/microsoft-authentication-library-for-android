package com.microsoft.identity.client;

import android.support.test.runner.AndroidJUnit4;

import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class DiscoveryTests {


    @Test
    public void testInstanceDiscovery() throws IOException {
        AndroidTestMockUtil.mockSuccessInstanceDiscoveryAPIVersion1_1();

        AzureActiveDirectory.performCloudDiscovery();


    }


}
