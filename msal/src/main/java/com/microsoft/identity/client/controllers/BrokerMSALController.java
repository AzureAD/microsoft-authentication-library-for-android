package com.microsoft.identity.client.controllers;

import android.content.Intent;

import com.microsoft.identity.client.AuthenticationResult;

public class BrokerMSALController extends MSALController {

    @Override
    public AuthenticationResult AcquireToken(MSALAcquireTokenOperationParameters request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void CompleteAcquireToken(int requestCode, int resultCode, Intent data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AuthenticationResult AcquireTokenSilent(MSALAcquireTokenSilentOperationParameters request) {
        throw new UnsupportedOperationException();
    }

}
