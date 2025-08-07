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

import com.microsoft.identity.common.java.flighting.IFlightConfig
import com.microsoft.identity.common.java.flighting.IFlightsProvider
import org.json.JSONObject

/**
 * Default implementation of [IFlightsProvider] that fetches flight configurations from the ECS service.
 * This provider is used for device-wide flight configurations in MSAL.
 */
class MsalDefaultFlightsProvider : IFlightsProvider {
    private val TAG = MsalDefaultFlightsProvider::class.java.simpleName

    // Cache for flight configuration values. For testing purposes I'm putting in one hardcoded flight.
    private val mBooleanFlights = mutableMapOf<String, Boolean>(
        "EnablePasskeyFeature" to false
    )
    private val mStringFlights = mutableMapOf<String, String>()
    private val mIntFlights = mutableMapOf<String, Int>()
    private val mJsonFlights = mutableMapOf<String, JSONObject>()
    private val mDoubleFlights = mutableMapOf<String, Double>()

    override fun isFlightEnabled(flightConfig: IFlightConfig): Boolean {
        return getBooleanValue(flightConfig)
    }

    override fun getBooleanValue(flightConfig: IFlightConfig): Boolean {
        return mBooleanFlights[flightConfig.key] ?: flightConfig.defaultValue as Boolean
    }

    override fun getStringValue(flightConfig: IFlightConfig): String? {
        return mStringFlights[flightConfig.key] ?: flightConfig.defaultValue as String
    }

    override fun getJsonValue(flightConfig: IFlightConfig): JSONObject {
        return mJsonFlights[flightConfig.key] ?: flightConfig.defaultValue as JSONObject
    }

    override fun getIntValue(flightConfig: IFlightConfig): Int {
        return mIntFlights[flightConfig.key] ?: flightConfig.defaultValue as Int
    }

    override fun getDoubleValue(flightConfig: IFlightConfig): Double {
        return mDoubleFlights[flightConfig.key] ?: flightConfig.defaultValue as Double
    }
}
