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

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.security.KeyStore;

interface IDevice {

    /**
     * Tests if the supplied {@link AuthenticationScheme} is supported in the current runtime.
     *
     * @param scheme The AuthenticationScheme to check.
     * @return True if the provided AuthenticationScheme is supported. False otherwise.
     */
    boolean isAuthenticationSchemeSupported(@NonNull final AuthenticationScheme scheme);

    /**
     * Gets the API level of the current runtime.
     *
     * @return The API level.
     */
    int getApiLevel();

    /**
     * Gets the make of the current device.
     *
     * @return The name of the device manufacturer.
     */
    String getMake();

    /**
     * Gets the model name/code of the current device.
     *
     * @return The device model name.
     */
    String getModel();

    /**
     * Returns A KeyStore of type "AndroidKeyStore" that is private to this application.
     *
     * @return A reference to the AndroidKeyStore singleton.
     */
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    KeyStore getAndroidKeyStore();
}
