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

import com.microsoft.identity.msal.BuildConfig;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import static com.microsoft.identity.client.EventConstants.EventProperty;
import static com.microsoft.identity.client.PlatformIdHelper.PlatformIdParameters;

/**
 * A DefaultEvent stores Event data common to an Application or to a series of Events.
 */
class DefaultEvent extends Event implements IDefaultEvent {

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
    DefaultEvent(Builder builder) {
        super(builder);
        setProperty(EventProperty.APPLICATION_NAME, sAllDefaults.mApplicationName);
        setProperty(EventProperty.APPLICATION_VERSION, sAllDefaults.mApplicationVersion);
        setProperty(EventProperty.CLIENT_ID, sAllDefaults.mClientId);
        setProperty(EventProperty.DEVICE_ID, sAllDefaults.mDeviceId);
        setProperty(EventProperty.SDK_VERSION, sAllDefaults.mSdkVersion);
        setProperty(EventProperty.SDK_PLATFORM, sAllDefaults.mSdkPlatform);
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

    /**
     * Builder for DefaultEvent instances.
     */
    static class Builder extends Event.Builder<Builder> {

        Builder() {
            super(EventName.DEFAULT_EVENT);
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

        private final String mApplicationName;
        private final String mApplicationVersion;
        private final String mClientId;
        private final String mDeviceId;
        private final String mSdkVersion;
        private final String mSdkPlatform;

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
            mSdkVersion = builder.mSdkVersion;
            mSdkPlatform = builder.mSdkPlatform;
        }

        /**
         * Generates a Defaults instance for the supplied {@link Context} and clientId.
         *
         * @param context  the {@link Context} from which these defaults should be created.
         * @param clientId the clientId of the application
         * @return the newly constructed EventDefaults instance.
         */
        @SuppressLint("HardwareIds")
        static Defaults forApplication(final Context context, final String clientId) {
            final Builder defaultsBuilder = new Builder()
                    .setClientId(clientId)
                    .setApplicationName(context.getPackageName())
                    .setSdkVersion(BuildConfig.VERSION_NAME)
                    .setSdkPlatform(PlatformIdParameters.PRODUCT_NAME);
            try {
                String versionName = context
                        .getPackageManager()
                        .getPackageInfo(defaultsBuilder.mApplicationName, 0).versionName;
                versionName = null == versionName ? "NA" : versionName;
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
         * Builder object for Defaults.
         */
        static class Builder {

            private String mApplicationName;
            private String mApplicationVersion;
            private String mClientId;
            private String mDeviceId;
            private String mSdkVersion;
            private String mSdkPlatform;

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
             * Sets the sdk version.
             *
             * @param sdkVersion the sdk version to set.
             * @return the Builder instance.
             */
            Builder setSdkVersion(final String sdkVersion) {
                mSdkVersion = sdkVersion;
                return this;
            }

            /**
             * Sets the sdk platform.
             *
             * @param sdkPlatform the sdk platform to set.
             * @return the Builder instance.
             */
            Builder setSdkPlatform(final String sdkPlatform) {
                mSdkPlatform = sdkPlatform;
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
