package com.microsoft.identity.client.e2e.tests.mocked;

import android.content.Context;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.common.internal.controllers.BrokerMsalController;
import com.microsoft.identity.common.internal.controllers.LocalMSALController;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.controllers.BaseController;

import org.robolectric.annotation.Implements;

@Implements(MSALControllerFactory.class)
public class ShadowMockMsalControllerFactory {

    public static boolean sRouteRequestToBrokerMsalController = false;

    public static BaseController getDefaultController(@NonNull final Context applicationContext,
                                                      @NonNull final Authority authority,
                                                      @NonNull final PublicClientApplicationConfiguration applicationConfiguration)
            throws MsalClientException {
        if (sRouteRequestToBrokerMsalController){
            return new BrokerMsalController(applicationContext);
        } else {
            return new LocalMSALController();
        }
    }
}
