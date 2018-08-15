package com.microsoft.identity.client.controllers;

import com.microsoft.identity.client.AuthenticationResult;

public interface MSALTokenOperation {
    public AuthenticationResult execute();
}
