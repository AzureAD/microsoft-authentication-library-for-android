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
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

/**
 * Tests for {@link PlatformIdHelper}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class PlatformIdHelperTest {
    @Test
    public void testPlatformIdHelperParams() {
        final Map<String, String> platformParams = PlatformIdHelper.getPlatformIdParameters();

        Assert.assertTrue(platformParams.get(PlatformIdHelper.PlatformIdParameters.PRODUCT).equals(
                PlatformIdHelper.PlatformIdParameters.PRODUCT_NAME));
        Assert.assertTrue(platformParams.get(PlatformIdHelper.PlatformIdParameters.VERSION).equals(
                PublicClientApplication.getSdkVersion()));

        final String buildCPU;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            buildCPU = Build.CPU_ABI;
        } else {
            buildCPU = Build.SUPPORTED_ABIS[0];
        }
        Assert.assertTrue(platformParams.get(PlatformIdHelper.PlatformIdParameters.CPU_PLATFORM).equals(buildCPU));

        Assert.assertTrue(platformParams.get(PlatformIdHelper.PlatformIdParameters.OS).equals(
                String.valueOf(Build.VERSION.SDK_INT)));
        Assert.assertTrue(platformParams.get(PlatformIdHelper.PlatformIdParameters.DEVICE_MODEL).equals(Build.MODEL));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPlatformIdHelperReturnUnmodifiableMap() {
        final Map<String, String> platformParams = PlatformIdHelper.getPlatformIdParameters();
        platformParams.put("somekey", "somevalue");
    }
}
