package com.microsoft.identity.client.controllers;

import android.content.Context;
import android.content.Intent;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.AuthenticationResult;

public class MSALTokenCommand implements MSALTokenOperation {

    private MSALOperationParameters parameters;
    private MSALController controller;
    private Context context;
    private AuthenticationCallback callback;


    @Override
    public AuthenticationResult execute() {
        if(getParameters() instanceof MSALAcquireTokenSilentOperationParameters){
            return getController().AcquireTokenSilent((MSALAcquireTokenSilentOperationParameters) getParameters());
        }else{
            throw new IllegalArgumentException("Invalid operation parameters");
        }
    }

    @Override
    public void notify(int requestCode, int resultCode, Intent data) {
        throw new UnsupportedOperationException();
    }


    public MSALOperationParameters getParameters() {
        return parameters;
    }

    public void setParameters(MSALOperationParameters parameters) {
        this.parameters = parameters;
    }

    public MSALController getController() {
        return controller;
    }

    public void setController(MSALController controller) {
        this.controller = controller;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public AuthenticationCallback getCallback() {
        return callback;
    }

    public void setCallback(AuthenticationCallback callback) {
        this.callback = callback;
    }
}
