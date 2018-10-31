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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.Application;
import android.content.Context;

import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.client.internal.authorities.AnyPersonalAccount;
import com.microsoft.identity.client.internal.authorities.Authority;
import com.microsoft.identity.client.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.client.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for returning the correct controller depending on the type of request (Silent, Interactive), authority
 * app configuration, device state
 */
public class MSALControllerFactory {

    /**
     * Returns the appropriate MSAL Controller depending on Authority, App and Device state
     *
     * 1) The client indicates it wants to use broker
     * 2) If not AAD Authority use local controller
     * 3) If the the authority is AAD and the Audience is instance of AnyPersonalAccount
     *    Use the local controller
     * 4) If broker is not installed use local controller
     * 5) Otherwise return broker controller
     *
     * @return
     */
    public static MSALController getAcquireTokenController(Context applicationContext, Authority authority, PublicClientApplicationConfiguration applicationConfiguration) {

        if(brokerEligible(applicationContext, authority, applicationConfiguration)){
            return new BrokerMSALController();
        }else{
            return new LocalMSALController();
        }

    }

    /**
     * Returns one or more controllers to address silent requests
     *
     * The order of the response matters.  The local controller should be returned first in order to
     * ensure that any local refresh tokens are preferred over the use of the broker
     *
     * Only return the broker controller when the following are true:
     *
     * 1) The client indicates it wants to use broker
     * 2) The authority is AAD
     * 3) The audience is not AnyPersonalAccount
     * 4) The broker is installed
     * 5) The broker redirect URI for the client is registered
     * @return
     */
    public static List<MSALController> getAcquireTokenSilentControllers(Context applicationContext, Authority authority, PublicClientApplicationConfiguration applicationConfiguration) {

        List<MSALController> controllers = new ArrayList<MSALController>();
        controllers.add(new LocalMSALController());
        if(brokerEligible(applicationContext, authority, applicationConfiguration)) {
            controllers.add(new BrokerMSALController());
        }

        return controllers;

    }

    /**
     * Determine if request is eligible to use the broker
     *
     * Client indicates that it wants to use broker
     * Authority == AzureActiveDirectoryAuthority
     * Audience != AnyPersonalAccounts
     * Broker Installed & Verified
     *
     *
     * @param applicationContext
     * @param authority
     * @param applicationConfiguration
     * @return
     */
    public static boolean brokerEligible(Context applicationContext, Authority authority, PublicClientApplicationConfiguration applicationConfiguration){

        //If app has asked for Broker or if the authority is not AAD return false
        if(!applicationConfiguration.getUseBroker() || !(authority instanceof AzureActiveDirectoryAuthority) ){
            return false;
        }

        //Do not use broker when the audience is MSA only (personal accounts / consumers tenant alias)
        AzureActiveDirectoryAuthority azureActiveDirectoryAuthority = (AzureActiveDirectoryAuthority)authority;
        if(azureActiveDirectoryAuthority.getAudience() instanceof AnyPersonalAccount){
            return false;
        }

        // Use broker if installed and verified
        if(brokerInstalled(applicationContext)){
            return true;
        }


        return false;
    }

    /**
     * Check if a broker is installed and trusted:
     * - Check that authenticator is available for custom account type "Work Account"
     * - Verify that the signature of package associated with the authenticator is trusted
     *
     * there may be multiple packages containing the android authenticator implementation (custom account)
     * but there is only one entry for custom account type currently registered by the AccountManager.
     * If another app tries to install same authenticator (custom account type) type, it will
     * queue up and will be active after first one is uninstalled.
     * @param applicationContext
     * @return
     */
    private static boolean brokerInstalled(Context applicationContext) {

        BrokerValidator brokerValidator = new BrokerValidator(applicationContext);
        AccountManager accountManager = AccountManager.get(applicationContext);

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
