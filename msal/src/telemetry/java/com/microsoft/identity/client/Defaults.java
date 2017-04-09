package com.microsoft.identity.client;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;

import com.microsoft.identity.msal.BuildConfig;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

/**
 * Data-container used for default Event values.
 */
final class Defaults {

    final String mApplicationName;
    final String mApplicationVersion;
    final String mClientId;
    final String mDeviceId;
    final String mSdkVersion;
    final String mSdkPlatform;

    /**
     * Constructs a new Defaults from the supplied Builder.
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
                .setSdkPlatform(PlatformIdHelper.PlatformIdParameters.PRODUCT_NAME);
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
