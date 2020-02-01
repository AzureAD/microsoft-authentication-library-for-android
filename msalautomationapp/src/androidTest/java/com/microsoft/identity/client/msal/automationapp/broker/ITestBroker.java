package com.microsoft.identity.client.msal.automationapp.broker;

public interface ITestBroker {

    boolean isBrokerOpen();

    void handleAccountPicker(String username);

    String brokerAppName();
}
