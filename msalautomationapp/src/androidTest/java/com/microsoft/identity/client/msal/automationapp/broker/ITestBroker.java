package com.microsoft.identity.client.msal.automationapp.broker;

import com.microsoft.identity.client.msal.automationapp.app.IApp;

public interface ITestBroker extends IApp {

    void handleAccountPicker(String username);

    void performDeviceRegistration(String username, String password);

    void performSharedDeviceRegistration(String username, String password);
}
