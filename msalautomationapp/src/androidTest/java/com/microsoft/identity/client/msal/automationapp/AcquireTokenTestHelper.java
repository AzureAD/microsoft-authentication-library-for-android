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
package com.microsoft.identity.client.msal.automationapp;

import android.content.Context;
import android.widget.Toast;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.internal.util.StringUtil;

import org.junit.Assert;

import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.fail;

public class AcquireTokenTestHelper {

    private static IAccount sAccount;

    public static IAccount getAccount() {
        return sAccount;
    }

    public static void setAccount(IAccount account) {
        sAccount = account;
    }

    public static AuthenticationCallback successfulInteractiveCallback(final CountDownLatch latch, final Context context) {
        AuthenticationCallback callback = new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Toast.makeText(context, authenticationResult.getAccessToken(), Toast.LENGTH_SHORT).show();
                Assert.assertFalse(StringUtil.isEmpty(authenticationResult.getAccessToken()));
                sAccount = authenticationResult.getAccount();
                latch.countDown();
            }

            @Override
            public void onError(MsalException exception) {
                fail(exception.getMessage());
                latch.countDown();
            }

            @Override
            public void onCancel() {
                fail("User cancelled flow");
                latch.countDown();
            }
        };

        return callback;
    }


    public static SilentAuthenticationCallback successfulSilentCallback(final CountDownLatch latch, final Context context) {
        SilentAuthenticationCallback callback = new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Toast.makeText(context, authenticationResult.getAccessToken(), Toast.LENGTH_SHORT).show();
                Assert.assertFalse(StringUtil.isEmpty(authenticationResult.getAccessToken()));
                sAccount = authenticationResult.getAccount();
                latch.countDown();
            }

            @Override
            public void onError(MsalException exception) {
                fail(exception.getMessage());
                latch.countDown();
            }
        };

        return callback;
    }

    public static AuthenticationCallback failureInteractiveCallback(final CountDownLatch latch, final String errorCode, final Context context) {
        AuthenticationCallback callback = new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                fail("Unexpected success");
                latch.countDown();
            }

            @Override
            public void onError(MsalException exception) {
                Toast.makeText(context, exception.getErrorCode(), Toast.LENGTH_SHORT).show();
                Assert.assertEquals(errorCode, exception.getErrorCode());
                latch.countDown();
            }

            @Override
            public void onCancel() {
                fail("User cancelled flow");
                latch.countDown();
            }
        };

        return callback;
    }

    public static SilentAuthenticationCallback failureSilentCallback(final CountDownLatch latch, final String errorCode, final Context context) {
        SilentAuthenticationCallback callback = new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                fail("Unexpected success");
                latch.countDown();
            }

            @Override
            public void onError(MsalException exception) {
                Toast.makeText(context, exception.getErrorCode(), Toast.LENGTH_SHORT).show();
                latch.countDown();
            }
        };

        return callback;
    }

}
