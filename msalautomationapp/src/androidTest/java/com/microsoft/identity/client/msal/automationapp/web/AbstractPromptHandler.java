package com.microsoft.identity.client.msal.automationapp.web;

import androidx.annotation.NonNull;

public abstract class AbstractPromptHandler implements IPromptHandler {

    protected ILoginComponentHandler loginComponentHandler;
    protected PromptHandlerParameters parameters;

    public AbstractPromptHandler(@NonNull final ILoginComponentHandler loginComponentHandler,
                                 @NonNull final PromptHandlerParameters parameters) {
        this.loginComponentHandler = loginComponentHandler;
        this.parameters = parameters;
    }
}
