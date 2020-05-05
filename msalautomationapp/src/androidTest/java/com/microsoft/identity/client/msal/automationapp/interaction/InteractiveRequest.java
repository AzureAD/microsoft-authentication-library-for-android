package com.microsoft.identity.client.msal.automationapp.interaction;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.IPublicClientApplication;

public class InteractiveRequest {

    private IPublicClientApplication application;
    private AcquireTokenParameters parameters;
    private OnInteractionRequired interactionRequiredCallback;

    public InteractiveRequest(@NonNull final IPublicClientApplication application,
                              @NonNull final AcquireTokenParameters parameters,
                              @NonNull final OnInteractionRequired interactionRequiredCallback) {
        this.application = application;
        this.parameters = parameters;
        this.interactionRequiredCallback = interactionRequiredCallback;
    }

    public void execute() {
        application.acquireToken(parameters);
        interactionRequiredCallback.handleUserInteraction();
    }

}
