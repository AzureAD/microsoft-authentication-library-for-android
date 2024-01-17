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
package com.microsoft.identity.nativeauth;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.client.AndroidTestUtil;
import com.microsoft.identity.client.BrowserTabActivity;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.PublicClientApplicationTest;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.internal.MsalUtils;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.java.net.HttpUrlConnectionFactory;
import com.microsoft.identity.msal.test.R;
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

/**
 * Tests for {@link com.microsoft.identity.client.PublicClientApplication}.
 */
@RunWith(AndroidJUnit4.class)
public final class NativeAuthPublicClientApplicationTest {
    private Context mAppContext;
    private static final String CLIENT_ID = "client-id";
    private static final String CIAM_AUTHORITY = "https://msidlabciam1.ciamlogin.com/msidlabciam1.onmicrosoft.com";
    private final List<String> CHALLENGE_TYPES = Arrays.asList("oob", "password");

    @Before
    public void setUp() {
        System.setProperty(
                "dexmaker.dexcache",
                androidx.test.platform.app.InstrumentationRegistry
                        .getInstrumentation()
                        .getTargetContext()
                        .getCacheDir()
                        .getPath()
        );

        System.setProperty(
                "org.mockito.android.target",
                ApplicationProvider
                        .getApplicationContext()
                        .getCacheDir()
                        .getPath()
        );

        mAppContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext().getApplicationContext();
    }

    @After
    public void tearDown() {
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
        AndroidTestUtil.removeAllTokens(mAppContext);
    }

    @Test
    public void testNativeAuthConstructor() {
        final Context context = new PublicClientApplicationTest.MockContext(mAppContext);
        PublicClientApplicationTest.mockPackageManagerWithDefaultFlag(context, mAppContext);
        PublicClientApplicationTest.mockHasCustomTabRedirect(context);

        try {
            final INativeAuthPublicClientApplication app = PublicClientApplication.createNativeAuthPublicClientApplication(
                    context,
                    R.raw.test_msal_config_native_auth
            );
            Assert.assertNotNull(app);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        } catch (MsalException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testNativeAuthConstructorNoMetadata() {
        final Context context = new PublicClientApplicationTest.MockContext(mAppContext);
        PublicClientApplicationTest.mockPackageManagerWithDefaultFlag(context, mAppContext);
        PublicClientApplicationTest.mockHasCustomTabRedirect(context);

        try {
            final INativeAuthPublicClientApplication app = PublicClientApplication.createNativeAuthPublicClientApplication(
                    context,
                    CLIENT_ID,
                    CIAM_AUTHORITY,
                    null,
                    CHALLENGE_TYPES
            );
            Assert.assertNotNull(app);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        } catch (MsalException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testFailingNativeAuthConstructor() {
        final Context context = new PublicClientApplicationTest.MockContext(mAppContext);
        PublicClientApplicationTest.mockPackageManagerWithDefaultFlag(context, mAppContext);
        PublicClientApplicationTest.mockHasCustomTabRedirect(context);

        // Should fail, as we are attempting to create a multiple account application
        try {
            final INativeAuthPublicClientApplication app = PublicClientApplication.createNativeAuthPublicClientApplication(
                    context,
                    R.raw.test_msal_config_multiple_account
            );
            Assert.fail("Unintentionally created app");
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        } catch (MsalException e) {
            Assert.assertEquals("Expecting error.",
                    e.getErrorCode(),
                    MsalClientException.NATIVE_AUTH_INVALID_ACCOUNT_MODE_CONFIG_ERROR_CODE);
        }
    }
}