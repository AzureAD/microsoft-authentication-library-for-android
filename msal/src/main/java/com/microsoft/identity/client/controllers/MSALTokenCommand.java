package com.microsoft.identity.client.controllers;

import android.content.Context;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.AuthenticationResult;

public class MSALTokenCommand implements MSALTokenOperation {

    protected MSALOperationParameters parameters;
    protected MSALController controller;
    protected Context context;
    protected AuthenticationCallback callback;


    @Override
    public AuthenticationResult execute() {
        if(parameters instanceof MSALAcquireTokenSilentOperationParameters){
            return controller.AcquireTokenSilent((MSALAcquireTokenSilentOperationParameters)parameters);
        }else{
            throw new IllegalArgumentException("Invalid operation parameters");
        }
    }
}
