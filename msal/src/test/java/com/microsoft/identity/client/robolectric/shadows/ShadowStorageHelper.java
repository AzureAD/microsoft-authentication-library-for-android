package com.microsoft.identity.client.robolectric.shadows;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.adal.internal.cache.StorageHelper;

import org.robolectric.annotation.Implements;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;

@Implements(StorageHelper.class)
public class ShadowStorageHelper {

    /**
     * Fake saving key to key store as Android Key Store is not available in Robolectric
     */
    public void saveKeyStoreEncryptedKey(@NonNull SecretKey unencryptedKey) throws GeneralSecurityException, IOException {
        return;
    }

}
