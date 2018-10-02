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

import android.content.Intent;
import android.os.Handler;
import android.util.Pair;

import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalUserCancelException;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MSALApiDispatcher {

    private static final String TAG = MSALApiDispatcher.class.getSimpleName();

    private static final ExecutorService sInteractiveExecutor = Executors.newSingleThreadExecutor();
    private static final ExecutorService sSilentExecutor = Executors.newCachedThreadPool();
    private static final Object sLock = new Object();
    private static MSALInteractiveTokenCommand sCommand = null;

    public static void beginInteractive(final MSALInteractiveTokenCommand command) {
        final String methodName = ":beginInteractive";
        Logger.verbose(
                TAG + methodName,
                "Beginning interactive request"
        );
        synchronized (sLock) {
            sInteractiveExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    initializeDiagnosticContext();

                    if (command.mParameters instanceof MSALAcquireTokenOperationParameters) {
                        logInteractiveRequestParameters(methodName, (MSALAcquireTokenOperationParameters) command.mParameters);
                    }

                    sCommand = command;
                    AcquireTokenResult result = null;
                    MsalException msalException = null;

                    try {
                        //Try executing request
                        result = command.execute();
                    } catch (Exception e) {
                        //Capture any resulting exception and map to MsalException type
                        Logger.errorPII(
                                TAG + methodName,
                                "Interactive request failed with Exception",
                                e
                        );
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

    private static void logInteractiveRequestParameters(final String methodName,
                                                        final MSALAcquireTokenOperationParameters params) {
        com.microsoft.identity.common.internal.logging.Logger.verbose(
                TAG + methodName,
                "Requested "
                        + params.getScopes().size()
                        + " scopes"
        );

        com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                TAG + methodName,
                "----\nRequested scopes:"
        );
        for (final String scope : params.getScopes()) {
            com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                    TAG + methodName,
                    "\t" + scope
            );
        }
        com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                TAG + methodName,
                "----"
        );
        com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                TAG + methodName,
                "ClientId: [" + params.getClientId() + "]"
        );
        com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                TAG + methodName,
                "RedirectUri: [" + params.getRedirectUri() + "]"
        );
        com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                TAG + methodName,
                "Login hint: [" + params.getLoginHint() + "]"
        );

        if (null != params.getExtraQueryStringParameters()) {
            com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                    TAG + methodName,
                    "Extra query params:"
            );
            for (final Pair<String, String> qp : params.getExtraQueryStringParameters()) {
                com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                        TAG + methodName,
                        "\t\"" + qp.first + "\":\"" + qp.second + "\""
                );
            }
        }

        if (null != params.getExtraScopesToConsent()) {
            com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                    TAG + methodName,
                    "Extra scopes to consent:"
            );
            for (final String extraScope : params.getExtraScopesToConsent()) {
                com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                        TAG + methodName,
                        "\t" + extraScope
                );
            }
        }

        com.microsoft.identity.common.internal.logging.Logger.verbose(
                TAG + methodName,
                "Using authorization agent: " + params.getAuthorizationAgent().toString()
        );

        if (null != params.getAccount()) {
            com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                    TAG + methodName,
                    "Using account: " + params.getAccount().getHomeAccountId()
            );
        }
    }

    private static void logSilentRequestParams(final String methodName,
                                               final MSALAcquireTokenSilentOperationParameters parameters) {
        com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                TAG + methodName,
                "ClientId: [" + parameters.getClientId() + "]"
        );
        com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                TAG + methodName,
                "----\nRequested scopes:"
        );

        for (final String scope : parameters.getScopes()) {
            com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                    TAG + methodName,
                    "\t" + scope
            );
        }
        com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                TAG + methodName,
                "----"
        );

        if (null != parameters.getAccount()) {
            com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                    TAG + methodName,
                    "Using account: " + parameters.getAccount().getHomeAccountId()
            );
        }

        com.microsoft.identity.common.internal.logging.Logger.verbose(
                TAG + methodName,
                "Force refresh? [" + parameters.getForceRefresh() + "]"
        );
    }

    public static void completeInteractive(int requestCode, int resultCode, final Intent data) {
        sCommand.notify(requestCode, resultCode, data);
    }

    public static void submitSilent(final MSALTokenCommand command) {
        final String methodName = ":submitSilent";
        Logger.verbose(
                TAG + methodName,
                "Beginning silent request"
        );
        sSilentExecutor.execute(new Runnable() {
            @Override
            public void run() {
                initializeDiagnosticContext();

                if (command.mParameters instanceof MSALAcquireTokenSilentOperationParameters) {
                    logSilentRequestParams(methodName, (MSALAcquireTokenSilentOperationParameters) command.mParameters);
                }

                AcquireTokenResult result = null;
                MsalException msalException = null;

                try {
                    //Try executing request
                    result = command.execute();
                } catch (Exception e) {
                    //Capture any resulting exception and map to MsalException type
                    Logger.errorPII(
                            TAG + methodName,
                            "Silent request failed with Exception",
                            e
                    );
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

    public static String initializeDiagnosticContext() {
        final String methodName = ":initializeDiagnosticContext";
        final String correlationId = UUID.randomUUID().toString();
        final com.microsoft.identity.common.internal.logging.RequestContext rc =
                new com.microsoft.identity.common.internal.logging.RequestContext();
        rc.put(DiagnosticContext.CORRELATION_ID, correlationId);
        DiagnosticContext.setRequestContext(rc);
        Logger.verbose(
                TAG + methodName,
                "Initialized new DiagnosticContext"
        );

        return correlationId;
    }
}
