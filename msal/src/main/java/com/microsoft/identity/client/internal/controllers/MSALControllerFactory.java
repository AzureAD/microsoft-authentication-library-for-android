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
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.authorities.AnyPersonalAccount;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.logging.Logger;

import java.util.ArrayList;
import java.util.List;

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
            return new BrokerMsalController();
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
            controllers.add(new BrokerMsalController());
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
        final String methodName = ":brokerEligible";
        final String logBrokerEligibleFalse = "Eligible to call broker? [false]";

        //If app has asked for Broker or if the authority is not AAD return false
        if (!applicationConfiguration.getUseBroker() || !(authority instanceof AzureActiveDirectoryAuthority)) {
            Logger.verbose(TAG + methodName, logBrokerEligibleFalse);
            Logger.verbose(TAG + methodName, "App does not ask for Broker or the authority is not AAD authority.");
            return false;
        }

        //Do not use broker when the audience is MSA only (personal accounts / consumers tenant alias)
        AzureActiveDirectoryAuthority azureActiveDirectoryAuthority = (AzureActiveDirectoryAuthority) authority;

        if (azureActiveDirectoryAuthority.getAudience() instanceof AnyPersonalAccount) {
            Logger.verbose(TAG + methodName, logBrokerEligibleFalse);
            Logger.verbose(TAG + methodName, "The audience is MSA only.");
            return false;
        }

        // Check if broker installed
        if (!brokerInstalled(applicationContext)) {
            Logger.verbose(TAG + methodName, logBrokerEligibleFalse);
            Logger.verbose(TAG + methodName, "Broker application is not installed.");
            return false;
        }

        // Check if MicrosoftAuthService supported or AccountManager permission granted
        if (BrokerMsalController.isMicrosoftAuthServiceSupported(applicationContext)
                || BrokerMsalController.isAccountManagerPermissionsGranted(applicationContext)) {
            Logger.verbose(TAG + methodName, "Eligible to call broker? [true]");
            return true;
        } else if (!BrokerMsalController.isMicrosoftAuthServiceSupported(applicationContext)
                && powerOptimizationEnabled(applicationContext)) {
            Logger.verbose(TAG + methodName, logBrokerEligibleFalse);
            Logger.warn(TAG + methodName, "Is bound service supported? [false]");
            Logger.warn(TAG + methodName, "Is the power optimization enabled? [true]");
            throw new MsalClientException(MsalClientException.BROKER_BIND_FAILURE, "Unable to connect to the broker.");
        } else {
            Logger.verbose(TAG + methodName, logBrokerEligibleFalse);
            Logger.warn(TAG + methodName, "Is bound service supported? [false]");
            Logger.warn(TAG + methodName, "Is the power optimization enabled? [false]");
            Logger.warn(TAG + methodName, "Is AccountManager permission missing? [true]");
            throw new MsalClientException(MsalClientException.BROKER_BIND_FAILURE, "Unable to connect to the broker.");
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static boolean powerOptimizationEnabled(@NonNull final Context applicationContext) {
        final String methodName = ":powerOptimizationEnabled";
        final String packageName = applicationContext.getPackageName();
        PowerManager pm = (PowerManager) applicationContext.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && null != pm) {
            final boolean isPowerOptimizationOn = !pm.isIgnoringBatteryOptimizations(packageName);
            Logger.verbose(TAG + methodName, "Is power optimization on? [" + isPowerOptimizationOn + "]");
            return isPowerOptimizationOn;
        } else {
            Logger.verbose(TAG + methodName, "Is power optimization on? [" + false + "]");
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
            if (authenticator.type.equals(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE)
                    && brokerValidator.verifySignature(authenticator.packageName)) {
                return true;
            }
        }

        return false;
    }
}
