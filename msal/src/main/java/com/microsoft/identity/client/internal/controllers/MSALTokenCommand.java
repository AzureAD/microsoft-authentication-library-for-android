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
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MSALTokenCommand implements MSALTokenOperation {

    private static final String TAG = MSALTokenCommand.class.getSimpleName();

    protected MSALOperationParameters mParameters;
    protected MSALController mController;
    protected List<MSALController> mControllers;
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

    public MSALTokenCommand(@NonNull final Context context,
                            @NonNull final MSALOperationParameters parameters,
                            @NonNull final List<MSALController> controllers,
                            @NonNull final AuthenticationCallback callback) {
        mContext = context;
        mParameters = parameters;
        mController = null;
        mControllers = controllers;
        mCallback = callback;

        if (!(mParameters instanceof MSALAcquireTokenSilentOperationParameters)) {
            throw new IllegalArgumentException("Invalid operation parameters");
        }
    }

    @Override
    public AcquireTokenResult execute() throws InterruptedException, ExecutionException, IOException, ClientException, MsalClientException, MsalArgumentException, MsalUiRequiredException {

        AcquireTokenResult result = null;
        final String methodName = ":execute";

        for(MSALController controller : mControllers) {
            try {
                com.microsoft.identity.common.internal.logging.Logger.verbose(
                        TAG + methodName,
                        "Executing with controller: " + controller.getClass().getSimpleName()
                );
                result = controller.acquireTokenSilent((MSALAcquireTokenSilentOperationParameters) getParameters());
                if(result.getSucceeded()){
                    com.microsoft.identity.common.internal.logging.Logger.verbose(
                            TAG + methodName,
                            "Executing with controller: " + controller.getClass().getSimpleName() + ": Succeeded"
                    );
                    return result;
                }
            }catch(MsalUiRequiredException e){
                if(e.getErrorCode() == MsalUiRequiredException.INVALID_GRANT){
                    continue;
                }else{
                    throw e;
                }
            }
        }

        return result;
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

    public List<MSALController> getControllers () { return mControllers; }

    public void setControllers(List<MSALController> controllers){ this.mControllers = controllers;}

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
