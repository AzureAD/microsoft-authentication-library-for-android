package com.microsoft.identity.client.msal.automationapp.testpass.network;

import com.microsoft.identity.internal.testutils.networkutils.NetworkTestStateManager;

import org.junit.Test;

import java.io.IOException;

public class TestCaseNetwork {

    @Test
    public void doSomething() throws IOException {
        NetworkTestStateManager.readCSVFile(getClass(), "network_input.csv");
    }
}
