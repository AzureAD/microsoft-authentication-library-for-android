package com.microsoft.identity.client.controllers;

import android.content.Intent;

import com.microsoft.identity.client.AuthenticationResult;

public interface MSALTokenOperation {
    public AuthenticationResult execute();
    public void notify(int requestCode, int resultCode, final Intent data);
}
