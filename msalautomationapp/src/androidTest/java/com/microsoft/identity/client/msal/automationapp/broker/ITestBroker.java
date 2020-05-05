package com.microsoft.identity.client.msal.automationapp.broker;

public interface ITestBroker {

    void handleAccountPicker(String username);

    String brokerAppName();

    void performDeviceRegistration(String username, String password);
}
