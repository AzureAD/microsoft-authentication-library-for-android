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

package com.microsoft.identity.client;

import android.util.Pair;

import java.util.Collection;
import java.util.UUID;

/**
 * Internal interface for Event telemetry data.
 */
interface IEvent extends Collection<Pair<String, String>> {

    /**
     * Sets the supplied telemetry properties on this event.
     *
     * @param propertyName  the name of the property to set.
     *                      See {@link com.microsoft.identity.client.EventConstants.EventProperty}.
     * @param propertyValue the value of the property to set.
     *                      See {@link com.microsoft.identity.client.EventConstants.EventProperty.Value}.
     */
    void setProperty(final String propertyName, final String propertyValue);

    /**
     * Gets the telemetry property by name.
     * See {@link com.microsoft.identity.client.EventConstants.EventProperty}.
     *
     * @param propertyName the name of the property to get
     * @return the property value
     */
    String getProperty(final String propertyName);

    /**
     * Gets the number of properties associated to this event. Includes defaults.
     *
     * @return the number of properties.
     */
    int getPropertyCount();

    /**
     * Gets the name of this application.
     *
     * @return the application's name.
     */
    String getApplicationName();

    /**
     * Gets the version of this application.
     *
     * @return the application's version.
     */
    String getApplicationVersion();

    /**
     * Gets the clientId (hashed).
     *
     * @return the hashed clientId.
     */
    String getClientId();

    /**
     * Gets the deviceId (hashed).
     *
     * @return the hashed deviceId
     */
    String getDeviceId();

    /**
     * Gets the {@link com.microsoft.identity.client.Telemetry.RequestId}.
     *
     * @return the RequestId to get
     */
    Telemetry.RequestId getRequestId();

    /**
     * Gets the {@link EventName}.
     *
     * @return the EventName to get
     */
    EventName getEventName();

    /**
     * Sets the {@link UUID} correlationId.
     *
     * @param correlationId the correlationId to set
     */
    void setCorrelationId(UUID correlationId);

    /**
     * Gets the {@link UUID} correlationId.
     *
     * @return the correlationId
     */
    UUID getCorrelationId();

}
