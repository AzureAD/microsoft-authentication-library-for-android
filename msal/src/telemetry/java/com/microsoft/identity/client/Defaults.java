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
     * Constructs a new Defaults
     */
    Defaults(final String applicationName, final String applicationVersion, final String clientId,
             final String deviceId, final String sdkVersion, final String sdkPlatform) {
        mApplicationName = applicationName;
        mApplicationVersion = applicationVersion;
        mClientId = clientId;
        mDeviceId = deviceId;
        mSdkVersion = sdkVersion;
        mSdkPlatform = sdkPlatform;
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
        final String applicationName = context.getPackageName();
        final String sdkVersion = BuildConfig.VERSION_NAME;
        final String sdkPlatform = PlatformIdHelper.PlatformIdParameters.PRODUCT_NAME;

        String applicationVersion;
        try {
            String versionName = context
                    .getPackageManager()
                    .getPackageInfo(applicationName, 0).versionName;
            applicationVersion = null == versionName ? "NA" : versionName;
        } catch (PackageManager.NameNotFoundException e) {
            applicationVersion = "NA";
        }

        String deviceId;
        try {
            deviceId = MsalUtils.createHash(
                    Settings.Secure.getString(
                            context.getContentResolver(),
                            Settings.Secure.ANDROID_ID
                    )
            );
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            deviceId = "";
        }

        return new Defaults(applicationName, applicationVersion, clientId, deviceId, sdkVersion, sdkPlatform);
    }
}
