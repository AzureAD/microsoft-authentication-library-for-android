package com.microsoft.identity.client.controllers;

import android.content.Intent;

import com.microsoft.identity.client.AuthenticationResult;

public class BrokerMSALController extends MSALController {

    @Override
    public AuthenticationResult acquireToken(MSALAcquireTokenOperationParameters request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void completeAcquireToken(int requestCode, int resultCode, Intent data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AuthenticationResult acquireTokenSilent(MSALAcquireTokenSilentOperationParameters request) {
        throw new UnsupportedOperationException();
    }
}
