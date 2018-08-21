package com.microsoft.identity.client;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.microsoft.identity.client.controllers.MSALAcquireTokenOperationParameters;
import com.microsoft.identity.client.controllers.MSALAcquireTokenSilentOperationParameters;
import com.microsoft.identity.client.controllers.MSALController;
import com.microsoft.identity.client.controllers.MSALInteractiveTokenCommand;
import com.microsoft.identity.client.controllers.MSALTokenCommand;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MSALApiDispatcher {

    private static final ExecutorService sInteractiveExecutor = Executors.newSingleThreadExecutor();
    private static final ExecutorService sSilentExecutor = Executors.newCachedThreadPool();
    private static final Object sLock = new Object();
    private static MSALInteractiveTokenCommand sCommand = null;

    public static void beginInteractive(final MSALInteractiveTokenCommand command){

        synchronized (sLock) {
            sInteractiveExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    sCommand = command;
                    final AuthenticationResult result = command.execute();
                    Handler handler = new Handler(command.getContext().getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            command.getCallback().onSuccess(result);
                        }
                    });
                }
            });
        }
    }

    public static void completeInteractive(int requestCode, int resultCode, final Intent data){
        sCommand.notify(requestCode, resultCode, data);
    }

    public static void submitSilent(final MSALTokenCommand command){
        sSilentExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final AuthenticationResult result = command.execute();
                Handler handler = new Handler(command.getContext().getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        command.getCallback().onSuccess(result);
                    }
                });
            }
        });
    }


}
