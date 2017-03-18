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

import java.util.ArrayList;

import static com.microsoft.identity.client.EventConstants.EventProperty;

class Event extends ArrayList<Pair<String, String>> implements IEvent {

    private static boolean sInitializeAllWithDefaults = false;
    private static EventDefaults sAllDefaults;

    static void initializeAllWithDefaults(final EventDefaults defaults) {
        sInitializeAllWithDefaults = true;
        sAllDefaults = defaults;
    }

    Event(final Builder builder) {
        if (sInitializeAllWithDefaults) {
            // add the defaults
            setProperty(EventProperty.APPLICATION_NAME, sAllDefaults.mApplicationName);
            setProperty(EventProperty.APPLICATION_VERSION, sAllDefaults.mApplicationVersion);
            setProperty(EventProperty.CLIENT_ID, sAllDefaults.mClientId);
            setProperty(EventProperty.DEVICE_ID, sAllDefaults.mDeviceId);
        }
        setProperty(EventProperty.REQUEST_ID, builder.mRequestId.value);
        setProperty(EventProperty.EVENT_NAME, builder.mEventName.value);
    }

    void setEventName(final EventName eventName) {
        setProperty(EventProperty.EVENT_NAME, eventName.value);
    }

    @Override
    public void setProperty(final String propertyName, final String propertyValue) {
        add(new Pair<>(propertyName, propertyValue));
    }

    @Override
    public String getProperty(final String propertyName) {
        String propertyValue = null;
        for (Pair<String, String> property : this) {
            if (property.first.equals(propertyName)) {
                propertyValue = property.first;
                break;
            }
        }
        return propertyValue;
    }

    @Override
    public int getPropertyCount() {
        return size();
    }

    static class Builder<T extends Builder> {

        private Telemetry.RequestId mRequestId;
        private EventName mEventName;

        T requestId(Telemetry.RequestId requestId) {
            mRequestId = requestId;
            return (T) this;
        }

        T eventName(EventName eventName) {
            mEventName = eventName;
            return (T) this;
        }

        IEvent build() {
            return new Event(this);
        }
    }

    static class EventDefaults {

        private String mApplicationName;
        private String mApplicationVersion;
        private String mClientId;
        private String mDeviceId;

        private EventDefaults(final Builder builder) {
            mApplicationName = builder.mApplicationName;
            mApplicationVersion = builder.mApplicationVersion;
            mClientId = builder.mClientId;
            mDeviceId = builder.mDeviceId;
        }

        static class Builder {

            private String mApplicationName;
            private String mApplicationVersion;
            private String mClientId;
            private String mDeviceId;

            Builder applicationName(final String applicationName) {
                mApplicationName = applicationName;
                return this;
            }

            Builder applicationVersion(final String applicationVersion) {
                mApplicationVersion = applicationVersion;
                return this;
            }

            Builder clientId(final String clientId) {
                mClientId = clientId;
                return this;
            }

            Builder deviceId(final String deviceId) {
                mDeviceId = deviceId;
                return this;
            }

            EventDefaults build() {
                return new EventDefaults(this);
            }
        }

    }

}
