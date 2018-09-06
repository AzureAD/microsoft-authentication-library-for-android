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

                    try {
                        result = command.execute();
                    }
                    catch(Exception e){

                        if(e instanceof MsalException){
                            Handler handler = new Handler(command.getContext().getMainLooper());
                            final MsalException msalException = (MsalException)e;
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    command.getCallback().onError(msalException);
                                }
                            });
                        }

                        //TODO: Map to MSALException Type
                    }

                    Handler handler = new Handler(command.getContext().getMainLooper());

                    if(result.getSucceeded()){
                        final AuthenticationResult authenticationResult = result.getAuthenticationResult();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                command.getCallback().onSuccess(authenticationResult);
                            }
                        });
                    }else{
                        final MsalException msalException = ExceptionAdapter.exceptionFromAcquireTokenResult(result);
                        if(msalException instanceof MsalUserCancelException){
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    command.getCallback().onCancel();
                                }
                            });
                        }else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    command.getCallback().onError(msalException);
                                }
                            });
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
                }catch(Exception e){
                    e.printStackTrace();
                }
                Handler handler = new Handler(command.getContext().getMainLooper());
                if(result.getSucceeded()) {
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
