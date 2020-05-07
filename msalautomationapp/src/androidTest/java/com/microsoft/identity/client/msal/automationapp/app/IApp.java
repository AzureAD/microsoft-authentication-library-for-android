package com.microsoft.identity.client.msal.automationapp.app;

public interface IApp {

    void install();

    void launch();

    void clear();

    void uninstall();

    void handleFirstRun();
}
