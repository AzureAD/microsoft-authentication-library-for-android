package com.microsoft.identity.client.controllers;

public abstract class MSALController {

    public abstract void AcquireToken(MSALAcquireTokenRequest request);

    public abstract void AcquireTokenSilent();

}
