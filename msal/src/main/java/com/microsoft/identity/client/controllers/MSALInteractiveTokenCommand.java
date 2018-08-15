package com.microsoft.identity.client.controllers;

import com.microsoft.identity.client.AuthenticationResult;

import java.util.concurrent.ExecutionException;

public class MSALInteractiveTokenCommand extends MSALTokenCommand {

    @Override
    public AuthenticationResult execute(){
        if(parameters instanceof MSALAcquireTokenOperationParameters){
            AuthenticationResult result = null;

            try {
                result = controller.AcquireToken((MSALAcquireTokenOperationParameters)parameters);
                return result;
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                return result;
            }

        }else{
            throw new IllegalArgumentException("Invalid operation parameters");
        }
    }
}
