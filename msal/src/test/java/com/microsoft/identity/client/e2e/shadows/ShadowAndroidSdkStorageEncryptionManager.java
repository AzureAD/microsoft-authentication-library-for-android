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
package com.microsoft.identity.client.e2e.shadows;

import com.microsoft.identity.common.crypto.AndroidAuthSdkStorageEncryptionManager;
import com.microsoft.identity.common.crypto.PredefinedKeyLoader;
import com.microsoft.identity.common.java.crypto.key.AES256KeyLoader;

import org.robolectric.annotation.Implements;

import java.util.ArrayList;
import java.util.List;

@Implements(AndroidAuthSdkStorageEncryptionManager.class)
public class ShadowAndroidSdkStorageEncryptionManager {

    final byte[] encryptionKey = new byte[]{22, 78, -69, -66, 84, -65, 119, -9, -34, -80, 60, 67, -12, -117, 86, -47, -84, -24, -18, 121, 70, 32, -110, 51, -93, -10, -93, -110, 124, -68, -42, -119};
    final AES256KeyLoader mUserDefinedKey = new PredefinedKeyLoader("MOCK_ALIAS", encryptionKey);

    public  AES256KeyLoader getKeyLoaderForEncryption() {
        return mUserDefinedKey;
    }

    public List<AES256KeyLoader> getKeyLoaderForDecryption(byte[] cipherText) {
        return new ArrayList<AES256KeyLoader>() {{
            add(mUserDefinedKey);
        }};
    }
}