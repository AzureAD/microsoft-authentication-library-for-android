package com.microsoft.identity.client.controllers;

import android.content.Intent;

import com.microsoft.identity.client.AuthenticationResult;

import java.util.concurrent.ExecutionException;

public class MSALInteractiveTokenCommand extends MSALTokenCommand {

    @Override
    public AuthenticationResult execute(){
        if(getParameters() instanceof MSALAcquireTokenOperationParameters){
            AuthenticationResult result = null;

            try {
                result = getController().AcquireToken((MSALAcquireTokenOperationParameters) getParameters());
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return result;
        }else{
            throw new IllegalArgumentException("Invalid operation parameters");
        }
    }

    @Override
    public void notify(int requestCode, int resultCode, final Intent data){
        getController().CompleteAcquireToken(requestCode, resultCode, data);
    }
}
