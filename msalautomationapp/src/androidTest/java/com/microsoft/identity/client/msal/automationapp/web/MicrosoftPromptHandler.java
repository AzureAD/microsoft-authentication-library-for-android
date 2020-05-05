package com.microsoft.identity.client.msal.automationapp.web;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.UiResponse;

public class MicrosoftPromptHandler extends AbstractPromptHandler {

    public MicrosoftPromptHandler(
            final PromptHandlerParameters parameters) {
        super(
                new MicrosoftLoginComponentHandler(),
                parameters
        );
    }

    public void handlePrompt(final String username, final String password) {
        if (!parameters.isLoginHintProvided()) {
            if (parameters.getBroker() != null && parameters.isExpectingNonZeroAccountsInBroker()) {
                parameters.getBroker().handleAccountPicker(username);
            } else if (parameters.isExpectingNonZeroAccountsInCookie()){
                loginComponentHandler.handleAccountPicker(username);
            } else {
                loginComponentHandler.handleEmailField(username);
            }
        }

        if (parameters.getPrompt() == Prompt.LOGIN || !parameters.isSessionExpected()) {
            loginComponentHandler.handlePasswordField(password);
        }

        if (parameters.isConsentPageExpected() || parameters.getPrompt() == Prompt.CONSENT) {
            final UiResponse consentPageResponse = parameters.getConsentPageResponse();
            if (consentPageResponse == UiResponse.ACCEPT) {
                loginComponentHandler.acceptConsent();
            } else {
                loginComponentHandler.declineConsent();
            }
        }

        if (parameters.isSpeedBumpExpected()) {
            loginComponentHandler.handleSpeedBump();
        }
    }
}
