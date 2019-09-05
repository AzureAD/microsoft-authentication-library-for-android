package com.microsoft.identity.client.robolectric.shadows;

import android.content.Context;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.internal.MsalUtils;

import org.robolectric.annotation.Implements;

@Implements(MsalUtils.class)
public class ShadowMsalUtils {

    // mocking this to assume we have custom tab redirect activity during tests
    public static boolean hasCustomTabRedirectActivity(@NonNull final Context context,
                                                       @NonNull final String url) {
        return true;
    }
}
