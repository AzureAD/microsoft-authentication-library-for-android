// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.flighting

import android.content.Context
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightsProvider
import java.util.concurrent.ConcurrentHashMap

/**
 * MSAL implementation of [IFlightsManager] that provides access to flight values for MSAL.
 * This class is responsible for initializing the flights provider and managing the flight configurations.
 */
object MsalFlightManager : IFlightsManager {
    private val TAG = MsalFlightManager::class.java.simpleName

    /**
     * Instance to hold global [IFlightsProvider] instance.
     * This is initialized once per life of process.
     */
    private lateinit var mDefaultFlightsProvider: IFlightsProvider

    /**
     * Holds the map of tenantId to [IFlightsProvider] instances.
     */
    private lateinit var mTenantFlightsProviders: ConcurrentHashMap<String, IFlightsProvider>

    /**
     * Initializes the [MsalFlightManager] instance that should be used in this instance of MSAL process.
     * This is first part of MsalFlightManager setup, post which flight values can be read from cache.
     */
    fun initializeFlightsManager() {
        mDefaultFlightsProvider = MsalDefaultFlightsProvider()
        CommonFlightsManager.initializeCommonFlightsManager(this)
    }

    /**
     * Gets the [IFlightsProvider] instance that should be used in this instance of MSAL process
     * and is applicable for device wide flights.
     */
    override fun getFlightsProvider(waitForConfigsWithTimeoutInMs: Long): IFlightsProvider {
        return mDefaultFlightsProvider
    }

    /**
     * Gets the [IFlightsProvider] instance for given tenant. Features using per tenant flights
     * should call this using tenantId they are interested in.
     * If the flights provider for the tenant is not already initialized, it will initialize it and return it.
     */
    override fun getFlightsProviderForTenant(tenantId: String, waitForConfigsWithTimeoutInMs: Long): IFlightsProvider {
        return MsalTenantSpecificFlightsProvider(tenantId)
    }
}
