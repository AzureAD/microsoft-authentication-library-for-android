package com.microsoft.identity.client.controllers;

import android.content.Intent;

import java.util.concurrent.ExecutionException;

public abstract class MSALController {

    public abstract void AcquireToken(MSALAcquireTokenRequest request) throws ExecutionException, InterruptedException;

    public abstract void CompleteAcquireToken(int requestCode, int resultCode, final Intent data);

    public abstract void AcquireTokenSilent(MSALAcquireTokenSilentRequest request);

}
