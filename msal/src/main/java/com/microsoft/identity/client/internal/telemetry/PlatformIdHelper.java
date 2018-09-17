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

package com.microsoft.identity.client.internal.telemetry;

import android.os.Build;

import com.microsoft.identity.client.PublicClientApplication;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * MSAL internal Helper class to add additional platform specific query parameters or headers for the request sent to sts.
 */
public final class PlatformIdHelper {

    /**
     * Private constructor to prevent a help class from being initiated.
     */
    private PlatformIdHelper() {
    }

    public static Map<String, String> getPlatformIdParameters() {
        final Map<String, String> platformParameters = new HashMap<>();

        platformParameters.put(PlatformIdParameters.PRODUCT, PlatformIdParameters.PRODUCT_NAME);
        platformParameters.put(PlatformIdParameters.VERSION, PublicClientApplication.getSdkVersion());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            platformParameters.put(PlatformIdParameters.CPU_PLATFORM, Build.CPU_ABI);
        } else {
            final String[] supportedABIs = Build.SUPPORTED_ABIS;
            if (supportedABIs != null && supportedABIs.length > 0) {
                platformParameters.put(PlatformIdParameters.CPU_PLATFORM, supportedABIs[0]);
            }
        }
        platformParameters.put(PlatformIdParameters.OS, String.valueOf(Build.VERSION.SDK_INT));
        platformParameters.put(PlatformIdParameters.DEVICE_MODEL, Build.MODEL);

        return Collections.unmodifiableMap(platformParameters);
    }

    public static final class PlatformIdParameters {
        /**
         * The String representing the sdk platform.
         */
        public static final String PRODUCT = "x-client-SKU";

        /**
         * The String representing the sdk platform name.
         */
        public static final String PRODUCT_NAME = "MSAL.Android";

        /**
         * The String representing the sdk version.
         */
        public static final String VERSION = "x-client-Ver";

        /**
         * The String representing the CPU for the device.
         */
        public static final String CPU_PLATFORM = "x-client-CPU";

        /**
         * The String representing the device OS.
         */
        public static final String OS = "x-client-OS";

        /**
         * The String representing the device model.
         */
        public static final String DEVICE_MODEL = "x-client-DM";
    }
}
