// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.e2e.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.dto.CredentialType;

import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate.CACHE_VALUE_SEPARATOR;

public class RoboTestUtils {

    private static final String SHARED_PREFERENCES_NAME = "com.microsoft.identity.client.account_credential_cache";

    private static String getCacheKeyForAccessToken(Map<String, ?> cacheValues) {
        for (Map.Entry<String, ?> cacheValue : cacheValues.entrySet()) {
            final String cacheKey = cacheValue.getKey();
            if (isAccessToken(cacheKey)) {
                return cacheKey;
            }
        }

        return null;
    }


    /**
     * Inspects the supplied cache key to determine the target CredentialType.
     *
     * @param cacheKey The cache key to inspect.
     * @return The CredentialType or null if a proper type cannot be resolved.
     */
    @Nullable
    private static CredentialType getCredentialTypeForCredentialCacheKey(@NonNull final String cacheKey) {
        if (StringExtensions.isNullOrBlank(cacheKey)) {
            throw new IllegalArgumentException("Param [cacheKey] cannot be null.");
        }

        final Set<String> credentialTypesLowerCase = new HashSet<>();

        for (final String credentialTypeStr : CredentialType.valueSet()) {
            credentialTypesLowerCase.add(credentialTypeStr.toLowerCase(Locale.US));
        }

        CredentialType type = null;
        for (final String credentialTypeStr : credentialTypesLowerCase) {
            if (cacheKey.contains(CACHE_VALUE_SEPARATOR + credentialTypeStr + CACHE_VALUE_SEPARATOR)) {
                if (credentialTypeStr.equalsIgnoreCase(CredentialType.AccessToken.name())) {
                    type = CredentialType.AccessToken;
                    break;
                } else if (credentialTypeStr.equalsIgnoreCase(CredentialType.RefreshToken.name())) {
                    type = CredentialType.RefreshToken;
                    break;
                } else if (credentialTypeStr.equalsIgnoreCase(CredentialType.IdToken.name())) {
                    type = CredentialType.IdToken;
                    break;
                } else if (credentialTypeStr.equalsIgnoreCase(CredentialType.V1IdToken.name())) {
                    type = CredentialType.V1IdToken;
                    break;
                }
            }
        }

        return type;
    }

    private static boolean isAccessToken(@NonNull final String cacheKey) {
        boolean isAccessToken = CredentialType.AccessToken == getCredentialTypeForCredentialCacheKey(cacheKey);
        return isAccessToken;
    }

    private static SharedPreferences getSharedPreferences() {
        final Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return sharedPreferences;
    }

    public static void clearCache() {
        SharedPreferences sharedPreferences = getSharedPreferences();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
    }

    public static void removeAccessTokenFromCache() {
        SharedPreferences sharedPreferences = getSharedPreferences();
        final Map<String, ?> cacheValues = sharedPreferences.getAll();
        final String keyToRemove = getCacheKeyForAccessToken(cacheValues);
        if (keyToRemove != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(keyToRemove);
            editor.commit();
        }
    }

    public static void flushScheduler() {
        // wait until all runnable(s) have finished executing
        while (!RuntimeEnvironment.getMasterScheduler().advanceToLastPostedRunnable()) ;
    }

    public static void flushSchedulerWithDelay(@NonNull final long sleepTime) {
        try {
            // just wait a little for runnable(s) to enter the queue
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // if there are no runnable(s) after the delay, then we can just return
        if (RuntimeEnvironment.getMasterScheduler().size() == 0) {
            return;
        }

        flushScheduler();
    }

    public static Activity getMockActivity(final Context context) {
        final Activity mockedActivity = Mockito.mock(Activity.class);
        Mockito.when(mockedActivity.getApplicationContext()).thenReturn(context);

        return mockedActivity;
    }

}
