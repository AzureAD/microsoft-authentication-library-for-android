
package com.microsoft.identity.client;

/**
 * Internal Util class for MSAL.
 */
final class MSALUtils {

    /**
     * Private constructor to prevent Util class from being initiated.
     */
    private MSALUtils() { }

    /**
     * To improve test-ability with local Junit. Android.jar used for local Junit doesn't have a default implementation
     * for {@link android.text.TextUtils}.
     */
    static boolean isEmpty(final String message) {
        return message == null || message.trim().length() == 0;
    }
}
