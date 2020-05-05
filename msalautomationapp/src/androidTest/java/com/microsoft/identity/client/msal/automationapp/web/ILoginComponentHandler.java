package com.microsoft.identity.client.msal.automationapp.web;

public interface ILoginComponentHandler {

    void handleEmailField(final String username);

    void handlePasswordField(final String password);

    void handleBackButton();

    void handleNextButton();

    void handleAccountPicker(final String username);

    void confirmConsentPageReceived();

    void acceptConsent();

    void declineConsent();

    void handleSpeedBump();
}
