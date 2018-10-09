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

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.exception.MsalArgumentException;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.logging.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class MSALInteractiveTokenCommand extends MSALTokenCommand {

    private static final String TAG = MSALInteractiveTokenCommand.class.getSimpleName();

    public MSALInteractiveTokenCommand(Context context, MSALOperationParameters parameters, MSALController controller, AuthenticationCallback callback) {
        mContext = context;
        mParameters = parameters;
        mController = controller;
        mCallback = callback;

        if (!(mParameters instanceof MSALAcquireTokenOperationParameters)) {
            throw new IllegalArgumentException("Invalid operation parameters");
        }
    }

    @Override
    public AcquireTokenResult execute() throws InterruptedException, ExecutionException, IOException, ClientException, MsalClientException, MsalArgumentException {
        final String methodName = ":execute";
        if (getParameters() instanceof MSALAcquireTokenOperationParameters) {
            Logger.info(
                    TAG + methodName,
                    "Executing interactive token command..."
            );
            return getController().acquireToken((MSALAcquireTokenOperationParameters) getParameters());
        } else {
            throw new IllegalArgumentException("Invalid operation parameters");
        }
    }

    @Override
    public void notify(int requestCode, int resultCode, final Intent data) {
        getController().completeAcquireToken(requestCode, resultCode, data);
    }
}
