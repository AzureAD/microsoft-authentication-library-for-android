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

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

/**
 * A DefaultEvent stores Event data common to an Application or to a series of Events.
 */
class DefaultEvent extends AbstractCorrelatableEvent implements IDefaultEvent {

    private static Defaults sAllDefaults;

    /**
     * Sets the default fields automatically for *all* Events.
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
    DefaultEvent(Builder builder) {
        super(builder);
        setProperty(EventConstants.EventProperty.APPLICATION_NAME, sAllDefaults.mApplicationName);
        setProperty(EventConstants.EventProperty.APPLICATION_VERSION, sAllDefaults.mApplicationVersion);
        setProperty(EventConstants.EventProperty.CLIENT_ID, sAllDefaults.mClientId);
        setProperty(EventConstants.EventProperty.DEVICE_ID, sAllDefaults.mDeviceId);
    }

    @Override
    public final String getApplicationName() {
        return getProperty(EventConstants.EventProperty.APPLICATION_NAME);
    }

    @Override
    public final String getApplicationVersion() {
        return getProperty(EventConstants.EventProperty.APPLICATION_VERSION);
    }

    @Override
    public final String getClientId() {
        return getProperty(EventConstants.EventProperty.CLIENT_ID);
    }

    @Override
    public final String getDeviceId() {
        return getProperty(EventConstants.EventProperty.DEVICE_ID);
    }

    /**
     * Builder for DefaultEvent instances.
     */
    static class Builder extends Event.Builder<Builder> {

        Builder(Telemetry.RequestId requestId) {
            super(requestId, EventName.DEFAULT_EVENT);
        }

        @Override
        IDefaultEvent build() {
            return new DefaultEvent(this);
        }
    }

    /**
     * Data-container used for default Event values.
     */
    static final class Defaults {

        private String mApplicationName;
        private String mApplicationVersion;
        private String mClientId;
        private String mDeviceId;

        /**
         * Constructs a new EventDefaults from the supplied Builder.
         *
         * @param builder the Builder to use in this construction.
         */
        Defaults(final Builder builder) {
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
        static Defaults forApplication(final Context context, final String clientId) {
            Builder defaultsBuilder = new Builder()
                    .setClientId(clientId)
                    .setApplicationName(context.getPackageName());
            try {
                String versionName = context
                        .getPackageManager()
                        .getPackageInfo(defaultsBuilder.mApplicationName, 0).versionName;
                versionName = null == versionName ? "" : versionName;
                defaultsBuilder.setApplicationVersion(versionName);
            } catch (PackageManager.NameNotFoundException e) {
                defaultsBuilder.setApplicationVersion("NA");
            }

            try {
                defaultsBuilder.setDeviceId(
                        MSALUtils.createHash(
                                Settings.Secure.getString(
                                        context.getContentResolver(),
                                        Settings.Secure.ANDROID_ID
                                )
                        )
                );
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                defaultsBuilder.setDeviceId("");
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
            Builder setApplicationName(final String applicationName) {
                mApplicationName = applicationName;
                return this;
            }

            /**
             * Sets the application version.
             *
             * @param applicationVersion the application version to set.
             * @return the Builder instance.
             */
            Builder setApplicationVersion(final String applicationVersion) {
                mApplicationVersion = applicationVersion;
                return this;
            }

            /**
             * Sets the clientId.
             *
             * @param clientId the clientId to set.
             * @return the Builder instance.
             */
            Builder setClientId(final String clientId) {
                mClientId = clientId;
                return this;
            }

            /**
             * Sets the deviceId.
             *
             * @param deviceId the deviceId to set.
             * @return the Builder instance.
             */
            Builder setDeviceId(final String deviceId) {
                mDeviceId = deviceId;
                return this;
            }

            /**
             * Constructs a new EventDefaults.
             *
             * @return the newly constructed EventDefaults instance.
             */
            Defaults build() {
                return new Defaults(this);
            }
        }

    }

}
