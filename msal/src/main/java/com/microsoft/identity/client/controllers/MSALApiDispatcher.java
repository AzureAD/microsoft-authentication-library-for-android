package com.microsoft.identity.client;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.microsoft.identity.client.controllers.MSALController;
import com.microsoft.identity.client.controllers.MSALAcquireTokenRequest;
import com.microsoft.identity.client.controllers.MSALAcquireTokenSilentRequest;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MSALApiDispatcher {

    private static final ExecutorService sInteractiveExecutor = Executors.newSingleThreadExecutor();
    private static final ExecutorService sSilentExecutor = Executors.newCachedThreadPool();
    private static final Object sLock = new Object();
    private static MSALController sInteractiveController = null;

    public static void BeginInteractive(final MSALController controller, final MSALAcquireTokenRequest request){

        synchronized (sLock) {
            sInteractiveExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    sInteractiveController = controller;
                    try {
                        controller.AcquireToken(request);
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public static void CompleteInteractive(int requestCode, int resultCode, final Intent data){
        sInteractiveController.CompleteAcquireToken(requestCode, resultCode, data);
    }

    public static void SubmitSilent(final MSALController controller, final MSALAcquireTokenSilentRequest request){
        sSilentExecutor.execute(new Runnable() {
            @Override
            public void run() {
                controller.AcquireTokenSilent(request);
            }
        });
    }
}
