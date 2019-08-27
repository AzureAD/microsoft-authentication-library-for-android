package com.microsoft.identity.client.shadows;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.adal.internal.cache.StorageHelper;

import org.robolectric.annotation.Implements;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;


// Robolectric provides Shadows that work in similar way to how mocks work in Mockito.
// Implementing a shadow for a specific class doesn't shadow the entire class,
// instead it only shadows the method that is implemented in the shadow class.
@Implements(StorageHelper.class)
public class ShadowStorageHelper {

    /**
     * Fake saving key to key store as Android Key Store is not available in Robolectric
     */
    public void saveKeyStoreEncryptedKey(@NonNull SecretKey unencryptedKey) throws GeneralSecurityException, IOException {
        return;
    }

}
