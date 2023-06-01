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
package com.microsoft.identity.client.internal.controllers

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.annotation.VisibleForTesting
import com.microsoft.identity.client.PublicClientApplicationConfiguration
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory
import com.microsoft.identity.common.internal.activebrokerdiscovery.BrokerDiscoveryClientFactory
import com.microsoft.identity.common.internal.controllers.BrokerMsalController
import com.microsoft.identity.common.internal.controllers.LocalMSALController
import com.microsoft.identity.common.java.authorities.Authority
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAuthority
import com.microsoft.identity.common.java.controllers.BaseController
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.logging.Logger
import com.microsoft.identity.msal.BuildConfig

class MSALControllerFactory(
    private val applicationContext: Context,
    private val platformComponents: IPlatformComponents,
    private val applicationConfiguration: PublicClientApplicationConfiguration) {

    private val discoveryClient = BrokerDiscoveryClientFactory.getInstance(
        context = applicationContext,
        platformComponents = platformComponents
    )

    constructor(applicationConfiguration: PublicClientApplicationConfiguration):
        this(applicationConfiguration.appContext,
             AndroidPlatformComponentsFactory.createFromContext(applicationConfiguration.appContext),
             applicationConfiguration)

    companion object {
        private val TAG = MSALControllerFactory::class.simpleName

        @JvmStatic
        @VisibleForTesting
        var injectedMockDefaultController: BaseController? = null
    }

    /**
     * Returns the appropriate MSAL Controller depending on Authority, App and Device state
     *
     * 1) The client indicates it wants to use broker
     * 2) If not AAD Authority use local controller
     * 3) If the the authority is AAD and the Audience is instance of AnyPersonalAccount
     * Use the local controller
     * 4) If broker is not installed use local controller
     * 5) Otherwise return broker controller
     */
    fun getDefaultController(authority: Authority): BaseController {
        if (BuildConfig.DEBUG) {
            injectedMockDefaultController?.let {
                return it
            }
        }

        val activeBroker = getActiveBrokerPackageName()
        return if (!activeBroker.isNullOrEmpty() && brokerEligible(authority)) {
            BrokerMsalController(applicationContext, platformComponents, activeBroker)
        } else {
            LocalMSALController()
        }
    }

    /**
     * Returns one or more controllers to address a given request.
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
     */
    fun getAllControllers(authority: Authority): List<BaseController> {
        val activeBroker = getActiveBrokerPackageName()
        val controllers: MutableList<BaseController> = ArrayList()
        controllers.add(LocalMSALController())
        if (!activeBroker.isNullOrEmpty() && brokerEligible(authority)) {
            controllers.add(
                BrokerMsalController(applicationContext, platformComponents, activeBroker)
            )
        }

        return controllers
    }

    /**
     * Returns true if the request is eligible to use broker (see [brokerEligible])
     * AND if a valid broker is found.
     **/
    fun brokerEligibleAndInstalled(authority: Authority): Boolean {
        return getActiveBrokerPackageName() != null && brokerEligible(authority)
    }

    /**
     * Determine if request is eligible to use the broker
     *
     * Client indicates that it wants to use broker
     * Authority == AzureActiveDirectoryAuthority
     * Audience != AnyPersonalAccounts
     *
     * Note: This method does NOT check if broker is installed.
     */
    private fun brokerEligible(authority: Authority): Boolean {
        val methodTag = "$TAG:brokerEligible"
        val logBrokerEligibleFalse = "Eligible to call broker? [false]. "

        //If app has not asked for Broker or if the authority is not AAD return false
        if (!applicationConfiguration.useBroker || authority !is AzureActiveDirectoryAuthority) {
            Logger.verbose(
                methodTag, logBrokerEligibleFalse +
                        "App does not ask for Broker or the authority is not AAD authority."
            )
            return false
        }

        if (powerOptimizationEnabled()) {
            Logger.verbose(methodTag, "Is the power optimization enabled? [true]")
        }
        return true
    }

    private fun powerOptimizationEnabled(): Boolean {
        val methodTag = "$TAG:powerOptimizationEnabled"
        val packageName = applicationContext.packageName
        val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val isPowerOptimizationOn = pm.isIgnoringBatteryOptimizations(packageName)
            Logger.verbose(
                methodTag,
                "Is power optimization on? [$isPowerOptimizationOn]"
            )
            isPowerOptimizationOn
        } else {
            Logger.verbose(methodTag, "Is power optimization on? [" + false + "]")
            false
        }
    }

    private fun getActiveBrokerPackageName(): String? {
        val methodTag = "$TAG:getActiveBrokerPackageName"
        val activeBroker = discoveryClient.getActiveBroker(shouldSkipCache = false)
        activeBroker?.let {
            return it.packageName
        }

        Logger.info(methodTag,"Broker application is not installed.")
        return null
    }
}