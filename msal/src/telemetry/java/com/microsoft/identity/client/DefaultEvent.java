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

import static com.microsoft.identity.client.EventConstants.EventProperty;

/**
 * A DefaultEvent stores Event data common to an Application or to a series of Events.
 */
final class DefaultEvent extends Event {

    private static Defaults sAllDefaults;

    /**
     * Sets the {@link Defaults} used to populate {@link DefaultEvent} properties.
     *
     * @param defaults the defaults to use.
     */
    static void initializeDefaults(final Defaults defaults) {
        sAllDefaults = defaults;
    }

    /**
     * Constructs a new Event.
     *
     * @param builder the Builder instance for this Event.
     */
    private DefaultEvent(Builder builder) {
        super(builder);
        setProperty(EventProperty.APPLICATION_NAME, sAllDefaults.mApplicationName);
        setProperty(EventProperty.APPLICATION_VERSION, sAllDefaults.mApplicationVersion);
        setProperty(EventProperty.CLIENT_ID, sAllDefaults.mClientId);
        setProperty(EventProperty.DEVICE_ID, sAllDefaults.mDeviceId);
        setProperty(EventProperty.SDK_VERSION, sAllDefaults.mSdkVersion);
        setProperty(EventProperty.SDK_PLATFORM, sAllDefaults.mSdkPlatform);
    }

    /**
     * Gets the application name.
     *
     * @return the application name to get.
     */
    String getApplicationName() {
        return getProperty(EventProperty.APPLICATION_NAME);
    }

    /**
     * Gets the application version.
     *
     * @return the application version to get.
     */
    String getApplicationVersion() {
        return getProperty(EventProperty.APPLICATION_VERSION);
    }

    /**
     * Gets the client id.
     *
     * @return the client id to get.
     */
    String getClientId() {
        return getProperty(EventProperty.CLIENT_ID);
    }

    /**
     * Gets the device id.
     *
     * @return the device id to get.
     */
    String getDeviceId() {
        return getProperty(EventProperty.DEVICE_ID);
    }

    /**
     * Gets the sdk version.
     *
     * @return the sdk version to get.
     */
    String getSdkVersion() {
        return getProperty(EventProperty.SDK_VERSION);
    }

    /**
     * Gets the sdk platform.
     *
     * @return the sdk platform to get.
     */
    String getSdkPlatform() {
        return getProperty(EventProperty.SDK_PLATFORM);
    }

    /**
     * Builder for DefaultEvent instances.
     */
    static class Builder extends Event.Builder<Builder> {

        Builder() {
            super(EventConstants.EventName.DEFAULT_EVENT);
        }

        @Override
        DefaultEvent build() {
            return new DefaultEvent(this);
        }
    }

}
