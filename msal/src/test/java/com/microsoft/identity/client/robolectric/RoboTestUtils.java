package com.microsoft.identity.client.robolectric;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.dto.CredentialType;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.Scheduler;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate.CACHE_VALUE_SEPARATOR;

public class RoboTestUtils {

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
        String prefName = "com.microsoft.identity.client.account_credential_cache";
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
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
        final Scheduler scheduler = RuntimeEnvironment.getMasterScheduler();
        while (!scheduler.advanceToLastPostedRunnable()) ;
    }


}
