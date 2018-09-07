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
package com.microsoft.identity.client;

import android.content.Intent;
import android.os.Handler;

import com.microsoft.identity.client.controllers.AcquireTokenResult;
import com.microsoft.identity.client.controllers.ExceptionAdapter;
import com.microsoft.identity.client.controllers.MSALInteractiveTokenCommand;
import com.microsoft.identity.client.controllers.MSALTokenCommand;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MSALApiDispatcher {

    private static final ExecutorService sInteractiveExecutor = Executors.newSingleThreadExecutor();
    private static final ExecutorService sSilentExecutor = Executors.newCachedThreadPool();
    private static final Object sLock = new Object();
    private static MSALInteractiveTokenCommand sCommand = null;

    public static void beginInteractive(final MSALInteractiveTokenCommand command) {
        synchronized (sLock) {
            sInteractiveExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    sCommand = command;
                    AcquireTokenResult result = null;
                    MsalException msalException = null;

                    try {
                        //Try executing request
                        result = command.execute();
                    } catch (Exception e) {
                        //Capture any resulting exception and map to MsalException type
                        if (e instanceof MsalException) {
                            msalException = (MsalException) e;
                        } else {
                            msalException = ExceptionAdapter.msalExceptionFromException(e);
                        }
                    }

                    Handler handler = new Handler(command.getContext().getMainLooper());

                    if (msalException != null) {
                        //Post On Error
                        final MsalException finalException = msalException;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                command.getCallback().onError(finalException);
                            }
                        });
                    } else {
                        if (null != result && result.getSucceeded()) {
                            //Post Success
                            final AuthenticationResult authenticationResult = result.getAuthenticationResult();
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    command.getCallback().onSuccess(authenticationResult);
                                }
                            });
                        } else {
                            //Get MsalException from Authorization and/or Token Error Response
                            msalException = ExceptionAdapter.exceptionFromAcquireTokenResult(result);
                            final MsalException finalException = msalException;
                            if (finalException instanceof MsalUserCancelException) {
                                //Post Cancel
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        command.getCallback().onCancel();
                                    }
                                });
                            } else {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        command.getCallback().onError(finalException);
                                    }
                                });
                            }
                        }

                    }
                }
            });
        }
    }


    public static void completeInteractive(int requestCode, int resultCode, final Intent data) {
        sCommand.notify(requestCode, resultCode, data);
    }

    public static void submitSilent(final MSALTokenCommand command) {
        sSilentExecutor.execute(new Runnable() {
            @Override
            public void run() {
                AcquireTokenResult result = null;

                try {
                    result = command.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Handler handler = new Handler(command.getContext().getMainLooper());

                if (result.getSucceeded()) {
                    final AuthenticationResult authenticationResult = result.getAuthenticationResult();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            command.getCallback().onSuccess(authenticationResult);
                        }
                    });
                }
            }
        });
    }


}
