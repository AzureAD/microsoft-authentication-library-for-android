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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Pair;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.UUID;

import static com.microsoft.identity.client.EventConstants.EventProperty;
import static com.microsoft.identity.client.EventConstants.EventProperty.Index;

/**
 * Internal base-class for Event telemetry data.
 */
class Event extends ArrayList<Pair<String, String>> implements IEvent {

    private static boolean sInitializeAllWithDefaults = false;
    private static EventDefaults sAllDefaults;

    /**
     * Sets the default fields automatically for *all* Events.
     *
     * @param defaults the defaults to use.
     */
    static void initializeAllWithDefaults(final EventDefaults defaults) {
        sInitializeAllWithDefaults = true;
        sAllDefaults = defaults;
    }

    /**
     * Constructs a new Event.
     *
     * @param builder the Builder instance for this Event.
     */
    Event(final Builder builder) {
        if (null == builder.mEventName) {
            throw new IllegalStateException("Event must have a name");
        }

        setProperty(Index.EVENT_NAME, EventProperty.EVENT_NAME, builder.mEventName.toString());

        if (sInitializeAllWithDefaults) {
            // add the defaults
            setProperty(Index.APPLICATION_NAME, EventProperty.APPLICATION_NAME, sAllDefaults.mApplicationName);
            setProperty(Index.APPLICATION_VERSION, EventProperty.APPLICATION_VERSION, sAllDefaults.mApplicationVersion);
            setProperty(Index.CLIENT_ID, EventProperty.CLIENT_ID, sAllDefaults.mClientId);
            setProperty(Index.DEVICE_ID, EventProperty.DEVICE_ID, sAllDefaults.mDeviceId);
        }

        setProperty(Index.REQUEST_ID, EventProperty.REQUEST_ID, builder.mRequestId.toString());

        if (null != builder.mCorrelationId) {
            setProperty(Index.CORRELATION_ID, EventProperty.CORRELATION_ID, builder.mCorrelationId.toString());
        }
    }

    @Override
    public void setProperty(final String propertyName, final String propertyValue) {
        if (!MSALUtils.isEmpty(propertyName) && !MSALUtils.isEmpty(propertyValue)) {
            add(new Pair<>(propertyName, propertyValue));
        }
    }

    @Override
    public void setProperty(final int position, final String propertyName, final String propertyValue) {
        if (!MSALUtils.isEmpty(propertyName) && !MSALUtils.isEmpty(propertyValue)) {
            add(position, new Pair<>(propertyName, propertyValue));
        }
    }

    @Override
    public String getProperty(final String propertyName) {
        String propertyValue = null;
        for (Pair<String, String> property : this) {
            if (property.first.equals(propertyName)) {
                propertyValue = property.second;
                break;
            }
        }
        return propertyValue;
    }

    @Override
    public int getPropertyCount() {
        return size();
    }

    @Override
    public final String getApplicationName() {
        return getProperty(EventProperty.APPLICATION_NAME);
    }

    @Override
    public final String getApplicationVersion() {
        return getProperty(EventProperty.APPLICATION_VERSION);
    }

    @Override
    public final String getClientId() {
        return getProperty(EventProperty.CLIENT_ID);
    }

    @Override
    public final String getDeviceId() {
        return getProperty(EventProperty.DEVICE_ID);
    }

    @Override
    public Telemetry.RequestId getRequestId() {
        return new Telemetry.RequestId(getProperty(EventProperty.REQUEST_ID));
    }

    @Override
    public EventName getEventName() {
        return new EventName(getProperty(EventProperty.EVENT_NAME));
    }

    @Override
    public UUID getCorrelationId() {
        UUID correlationId;
        try {
            correlationId = UUID.fromString(getProperty(EventProperty.CORRELATION_ID));
        } catch (NullPointerException | IllegalArgumentException e) {
            correlationId = null;
        }
        return correlationId;
    }

    /**
     * Builder object used for Events.
     *
     * @param <T> generic type parameter for Builder subtypes.
     */
    abstract static class Builder<T extends Builder> {

        private Telemetry.RequestId mRequestId;
        private final EventName mEventName;
        private String mCorrelationId;

        Builder(Telemetry.RequestId requestId, final EventName name) {
            if (!Telemetry.RequestId.isValid(requestId)) {
                throw new IllegalArgumentException("Invalid RequestId");
            }
            mRequestId = requestId;
            mEventName = name;
        }

        private static boolean isValidCorrelationId(final UUID uuid) {
            return null != uuid && isValidCorrelationId(uuid.toString());
        }

        private static boolean isValidCorrelationId(final String correlationId) {
            try {
                UUID.fromString(correlationId);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        /**
         * Sets the correlationId of the Event
         *
         * @param correlationId UUID of the action
         */
        final void correlationId(final UUID correlationId) {
            if (isValidCorrelationId(correlationId)) {
                mCorrelationId = correlationId.toString();
            }
        }

        /**
         * Sets the correlationId of the Event
         *
         * @param correlationId UUID of the action
         */
        final void correlationId(final String correlationId) {
            if (isValidCorrelationId(correlationId)) {
                mCorrelationId = correlationId;
            }
        }

        /**
         * Gets the {@link com.microsoft.identity.client.Telemetry.RequestId}.
         * assigned to this Builder.
         *
         * @return the requestId.
         */
        final Telemetry.RequestId getRequestId() {
            return mRequestId;
        }

        /**
         * Gets the {@link EventName}.
         *
         * @return the EventName to get.
         */
        final EventName getEventName() {
            return mEventName;
        }

        /**
         * Constructs a new Event.
         *
         * @return the newly constructed Event instance.
         */
        IEvent build() {
            return new Event(this);
        }
    }

    /**
     * Data-container used for default Event values.
     */
    static final class EventDefaults {

        private String mApplicationName;
        private String mApplicationVersion;
        private String mClientId;
        private String mDeviceId;

        /**
         * Constructs a new EventDefaults from the supplied Builder.
         *
         * @param builder the Builder to use in this construction.
         */
        private EventDefaults(final Builder builder) {
            mApplicationName = builder.mApplicationName;
            mApplicationVersion = builder.mApplicationVersion;
            mClientId = builder.mClientId;
            mDeviceId = builder.mDeviceId;
        }

        /**
         * Generates an EventDefaults instance for the supplied {@link Context} and clientId.
         *
         * @param context  the {@link Context} from which these defaults should be created.
         * @param clientId the clientId of the application
         * @return the newly constructed EventDefaults instance.
         */
        @SuppressLint("HardwareIds")
        static EventDefaults forApplication(final Context context, final String clientId) {
            Builder defaultsBuilder = new Builder()
                    .clientId(clientId)
                    .applicationName(context.getPackageName());
            try {
                String versionName = context
                        .getPackageManager()
                        .getPackageInfo(defaultsBuilder.mApplicationName, 0).versionName;
                versionName = null == versionName ? "" : versionName;
                defaultsBuilder.applicationVersion(versionName);
            } catch (PackageManager.NameNotFoundException e) {
                defaultsBuilder.applicationVersion("NA");
            }

            try {
                defaultsBuilder.deviceId(
                        MSALUtils.createHash(
                                Settings.Secure.getString(
                                        context.getContentResolver(),
                                        Settings.Secure.ANDROID_ID
                                )
                        )
                );
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                defaultsBuilder.deviceId("");
            }

            return defaultsBuilder.build();
        }

        /**
         * Builder object for EventDefaults.
         */
        static class Builder {

            private String mApplicationName;
            private String mApplicationVersion;
            private String mClientId;
            private String mDeviceId;

            /**
             * Sets the application name.
             *
             * @param applicationName the application name to set.
             * @return the Builder instance.
             */
            Builder applicationName(final String applicationName) {
                mApplicationName = applicationName;
                return this;
            }

            /**
             * Sets the application version.
             *
             * @param applicationVersion the application version to set.
             * @return the Builder instance.
             */
            Builder applicationVersion(final String applicationVersion) {
                mApplicationVersion = applicationVersion;
                return this;
            }

            /**
             * Sets the clientId.
             *
             * @param clientId the clientId to set.
             * @return the Builder instance.
             */
            Builder clientId(final String clientId) {
                mClientId = clientId;
                return this;
            }

            /**
             * Sets the deviceId.
             *
             * @param deviceId the deviceId to set.
             * @return the Builder instance.
             */
            Builder deviceId(final String deviceId) {
                mDeviceId = deviceId;
                return this;
            }

            /**
             * Constructs a new EventDefaults.
             *
             * @return the newly constructed EventDefaults instance.
             */
            EventDefaults build() {
                return new EventDefaults(this);
            }
        }

    }

}
