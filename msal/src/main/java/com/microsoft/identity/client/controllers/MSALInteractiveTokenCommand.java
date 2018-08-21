package com.microsoft.identity.client.controllers;

import android.content.Context;
import android.content.Intent;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.AuthenticationResult;

import java.util.concurrent.ExecutionException;

public class MSALInteractiveTokenCommand extends MSALTokenCommand {

    public MSALInteractiveTokenCommand(Context context, MSALOperationParameters parameters, MSALController controller, AuthenticationCallback callback){
        mContext = context;
        mParameters = parameters;
        mController = controller;
        mCallback = callback;

        if(!(mParameters instanceof MSALAcquireTokenSilentOperationParameters)){
            throw new IllegalArgumentException("Invalid operation parameters");
        }
    }

    @Override
    public AuthenticationResult execute(){
        if(getParameters() instanceof MSALAcquireTokenOperationParameters){
            AuthenticationResult result = null;

            try {
                result = getController().acquireToken((MSALAcquireTokenOperationParameters) getParameters());
            } catch (ExecutionException e) {
                //TODO: complete implementation
                e.printStackTrace();
            } catch (InterruptedException e) {
                //TODO: complete implementation
                e.printStackTrace();
            }

            return result;
        }else{
            throw new IllegalArgumentException("Invalid operation parameters");
        }
    }

    @Override
    public void notify(int requestCode, int resultCode, final Intent data){
        getController().completeAcquireToken(requestCode, resultCode, data);
    }
}
