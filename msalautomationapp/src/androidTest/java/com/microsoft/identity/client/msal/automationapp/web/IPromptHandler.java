package com.microsoft.identity.client.msal.automationapp.web;

public interface IPromptHandler {

    void handlePrompt(String username, String password);
}
