//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client.internal.controllers;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.exception.MsalArgumentException;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;
import com.microsoft.identity.common.exception.ClientException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class MSALTokenCommand implements MSALTokenOperation {

    protected MSALOperationParameters mParameters;
    protected MSALController mController;
    protected Context mContext;
    protected AuthenticationCallback mCallback;


    public MSALTokenCommand() {
    }

    public MSALTokenCommand(@NonNull final Context context,
                            @NonNull final MSALOperationParameters parameters,
                            @NonNull final MSALController controller,
                            @NonNull final AuthenticationCallback callback) {
        mContext = context;
        mParameters = parameters;
        mController = controller;
        mCallback = callback;

        if (!(mParameters instanceof MSALAcquireTokenSilentOperationParameters)) {
            throw new IllegalArgumentException("Invalid operation parameters");
        }
    }

    @Override
    public AcquireTokenResult execute() throws InterruptedException, ExecutionException, IOException, ClientException, MsalClientException, MsalArgumentException, MsalUiRequiredException {
        return getController().acquireTokenSilent((MSALAcquireTokenSilentOperationParameters) getParameters());
    }

    @Override
    public void notify(int requestCode, int resultCode, Intent data) {
        throw new UnsupportedOperationException();
    }

    public MSALOperationParameters getParameters() {
        return mParameters;
    }

    public void setParameters(MSALOperationParameters parameters) {
        if (!(parameters instanceof MSALAcquireTokenSilentOperationParameters)) {
            throw new IllegalArgumentException("Invalid operation parameters");
        }
        this.mParameters = parameters;
    }

    public MSALController getController() {
        return mController;
    }

    public void setController(MSALController controller) {
        this.mController = controller;
    }

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    public AuthenticationCallback getCallback() {
        return mCallback;
    }

    public void setCallback(AuthenticationCallback callback) {
        this.mCallback = callback;
    }
}
