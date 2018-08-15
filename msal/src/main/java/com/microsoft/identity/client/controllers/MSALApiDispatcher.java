package com.microsoft.identity.client;

import android.content.Intent;

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

    public static void BeginInteractive(final MSALInteractiveTokenCommand command){

        synchronized (sLock) {
            sInteractiveExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    sCommand = command;
                    AuthenticationResult result = command.execute();
                }
            });
        }
    }

    public static void CompleteInteractive(int requestCode, int resultCode, final Intent data){
        sInteractiveController.CompleteAcquireToken(requestCode, resultCode, data);
    }

    public static void SubmitSilent(final MSALController controller, final MSALAcquireTokenSilentOperationParameters request){
        sSilentExecutor.execute(new Runnable() {
            @Override
            public void run() {
                controller.AcquireTokenSilent(request);
            }
        });
    }
}
