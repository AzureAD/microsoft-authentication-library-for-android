package com.microsoft.identity.client.controllers;

import android.content.Context;
import android.content.Intent;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.AuthenticationResult;

public class MSALTokenCommand implements MSALTokenOperation {

    protected MSALOperationParameters mParameters;
    protected MSALController mController;
    protected Context mContext;
    protected AuthenticationCallback mCallback;


    public MSALTokenCommand(){}

    public MSALTokenCommand(Context context, MSALOperationParameters parameters, MSALController controller, AuthenticationCallback callback){
        mContext = context;
        mParameters = parameters;
        mController = controller;
        mCallback = callback;

        if(!(mParameters instanceof MSALAcquireTokenSilentOperationParameters)){
            throw new IllegalArgumentException("Invalid operation parameters");
        }
    }

    @Override
    public AuthenticationResult execute() {
       return getController().acquireTokenSilent((MSALAcquireTokenSilentOperationParameters) getParameters());
    }

    @Override
    public void notify(int requestCode, int resultCode, Intent data) {
        throw new UnsupportedOperationException();
    }


    public MSALOperationParameters getParameters() {
        return mParameters;
    }

    public void setParameters(MSALOperationParameters parameters) {
        if(!(parameters instanceof MSALAcquireTokenSilentOperationParameters)){
            throw new IllegalArgumentException("Invalid operation parameters");
        }
        this.mParameters = parameters;
    }

    public MSALController getController() {
        return mController;
    }

    public void setController(MSALController controller) {
        this.mController = controller;
    }

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    public AuthenticationCallback getCallback() {
        return mCallback;
    }

    public void setCallback(AuthenticationCallback callback) {
        this.mCallback = callback;
    }
}
