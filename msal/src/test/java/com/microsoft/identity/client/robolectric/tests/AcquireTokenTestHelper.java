package com.microsoft.identity.client.robolectric.tests;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.internal.util.StringUtil;

import org.junit.Assert;

import static junit.framework.Assert.fail;

public class AcquireTokenTestHelper {

    private static IAccount sAccount;

    static IAccount getAccount() {
        return sAccount;
    }

    static void setAccount(IAccount account) {
        sAccount = account;
    }

    static AuthenticationCallback successfulInteractiveCallback() {
        AuthenticationCallback callback = new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Assert.assertTrue(!StringUtil.isEmpty(authenticationResult.getAccessToken()));
                sAccount = authenticationResult.getAccount();
            }

            @Override
            public void onError(MsalException exception) {
                fail(exception.getMessage());
            }

            @Override
            public void onCancel() {
                fail("User cancelled flow");
            }
        };

        return callback;
    }

    static SilentAuthenticationCallback successfulSilentCallback() {
        SilentAuthenticationCallback callback = new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Assert.assertTrue(!StringUtil.isEmpty(authenticationResult.getAccessToken()));
            }

            @Override
            public void onError(MsalException exception) {
                fail(exception.getMessage());
            }
        };

        return callback;
    }

    static AuthenticationCallback failureInteractiveCallback() {
        AuthenticationCallback callback = new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                fail("Unexpected success");
            }

            @Override
            public void onError(MsalException exception) {
                Assert.assertTrue(true);
            }

            @Override
            public void onCancel() {
                fail("User cancelled flow");
            }
        };

        return callback;
    }

    static SilentAuthenticationCallback failureSilentCallback() {
        SilentAuthenticationCallback callback = new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                fail("Unexpected success");
            }

            @Override
            public void onError(MsalException exception) {
                Assert.assertTrue(true);
            }
        };

        return callback;
    }

}
