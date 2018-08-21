package com.microsoft.identity.client.controllers;

import android.content.Intent;

import com.microsoft.identity.client.AuthenticationResult;

import java.util.concurrent.ExecutionException;

public abstract class MSALController {

    public abstract AuthenticationResult acquireToken(MSALAcquireTokenOperationParameters request) throws ExecutionException, InterruptedException;

    public abstract void completeAcquireToken(int requestCode, int resultCode, final Intent data);

    public abstract AuthenticationResult acquireTokenSilent(MSALAcquireTokenSilentOperationParameters request);

}
