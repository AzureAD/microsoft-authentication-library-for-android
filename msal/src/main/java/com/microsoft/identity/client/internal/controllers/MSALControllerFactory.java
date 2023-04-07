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

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.common.java.authorities.AnyPersonalAccount;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.internal.controllers.BrokerMsalController;
import com.microsoft.identity.common.internal.controllers.LocalMSALController;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightManager;
import com.microsoft.identity.common.logging.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;

/**
 * Responsible for returning the correct controller depending on the type of request (Silent, Interactive), authority
 * app configuration, device state
 */
public class MSALControllerFactory {
    private static final String TAG = MSALControllerFactory.class.getName();

    /**
     * Returns the appropriate MSAL Controller depending on Authority, App and Device state
     * <p>
     * 1) The client indicates it wants to use broker
     * 2) If not AAD Authority use local controller
     * 3) If the the authority is AAD and the Audience is instance of AnyPersonalAccount
     * Use the local controller
     * 4) If broker is not installed use local controller
     * 5) Otherwise return broker controller
     *
     * @return
     */
    public static BaseController getDefaultController(@NonNull final Context applicationContext,
                                                      @NonNull final Authority authority,
                                                      @NonNull final PublicClientApplicationConfiguration applicationConfiguration)
            throws MsalClientException {
        if (brokerEligible(applicationContext, authority, applicationConfiguration)) {
            return new BrokerMsalController(applicationContext);
        } else {
            return new LocalMSALController();
        }
    }

    /**
     * Returns one or more controllers to address a given request.
     * <p>
     * The order of the response matters.  The local controller should be returned first in order to
     * ensure that any local refresh tokens are preferred over the use of the broker
     * <p>
     * Only return the broker controller when the following are true:
     * <p>
     * 1) The client indicates it wants to use broker
     * 2) The authority is AAD
     * 3) The audience is not AnyPersonalAccount
     * 4) The broker is installed
     * 5) The broker redirect URI for the client is registered
     *
     * @return
     */
    public static List<BaseController> getAllControllers(@NonNull final Context applicationContext,
                                                         @NonNull final Authority authority,
                                                         @NonNull final PublicClientApplicationConfiguration applicationConfiguration)
            throws MsalClientException {
        List<BaseController> controllers = new ArrayList<>();
        controllers.add(new LocalMSALController());
        if (brokerEligible(applicationContext, authority, applicationConfiguration)) {
            controllers.add(new BrokerMsalController(applicationContext));
        }

        return controllers;
    }

    /**
     * Determine if request is eligible to use the broker
     * <p>
     * Client indicates that it wants to use broker
     * Authority == AzureActiveDirectoryAuthority
     * Audience != AnyPersonalAccounts
     * Broker Installed & Verified
     *
     * @param applicationContext
     * @param authority
     * @param applicationConfiguration
     * @return
     */
    public static boolean brokerEligible(@NonNull final Context applicationContext,
                                         @NonNull Authority authority,
                                         @NonNull PublicClientApplicationConfiguration applicationConfiguration) throws MsalClientException {
        final String methodTag = TAG + ":brokerEligible";
        final String logBrokerEligibleFalse = "Eligible to call broker? [false]. ";

        //If app has not asked for Broker or if the authority is not AAD return false
        if (!applicationConfiguration.getUseBroker() || !(authority instanceof AzureActiveDirectoryAuthority)) {
            Logger.verbose( methodTag, logBrokerEligibleFalse +
                    "App does not ask for Broker or the authority is not AAD authority.");
            return false;
        }

        // Check if broker installed
        if (!brokerInstalled(applicationContext)) {
            Logger.verbose(methodTag, logBrokerEligibleFalse +
                    "Broker application is not installed.");
            return false;
        }

        if (powerOptimizationEnabled(applicationContext)) {
            Logger.verbose(methodTag, "Is the power optimization enabled? [true]");
        }

        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static boolean powerOptimizationEnabled(@NonNull final Context applicationContext) {
        final String methodTag = TAG + ":powerOptimizationEnabled";
        final String packageName = applicationContext.getPackageName();
        PowerManager pm = (PowerManager) applicationContext.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && null != pm) {
            final boolean isPowerOptimizationOn = !pm.isIgnoringBatteryOptimizations(packageName);
            Logger.verbose(methodTag, "Is power optimization on? [" + isPowerOptimizationOn + "]");
            return isPowerOptimizationOn;
        } else {
            Logger.verbose(methodTag, "Is power optimization on? [" + false + "]");
            return false;
        }
    }

    /**
     * Check if a broker is installed and trusted:
     * - Check that authenticator is available for custom account type "Work Account"
     * - Verify that the signature of package associated with the authenticator is trusted
     * <p>
     * there may be multiple packages containing the android authenticator implementation (custom account)
     * but there is only one entry for custom account type currently registered by the AccountManager.
     * If another app tries to install same authenticator (custom account type) type, it will
     * queue up and will be active after first one is uninstalled.
     *
     * @param applicationContext
     * @return
     */
    protected static boolean brokerInstalled(@NonNull final Context applicationContext) {
        BrokerValidator brokerValidator = new BrokerValidator(applicationContext);
        AccountManager accountManager = AccountManager.get(applicationContext);

        //Verify the signature
        AuthenticatorDescription[] authenticators = accountManager.getAuthenticatorTypes();
        for (AuthenticatorDescription authenticator : authenticators) {
            if (BROKER_ACCOUNT_TYPE.equals(authenticator.type)
                    && brokerValidator.verifySignature(authenticator.packageName)) {
                return true;
            }
        }

        return false;
    }
}
