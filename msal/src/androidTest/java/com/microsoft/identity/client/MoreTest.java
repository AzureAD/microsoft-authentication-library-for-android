package com.microsoft.identity.client;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.test.runner.AndroidJUnit4;

import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.internal.util.StringUtil;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public final class MoreTest {

    private static final String[] SCOPES = {"user.read", "openid", "offline_access", "profile"};
    private static final String AAD_ROPC_TEST_AUTHORITY = "https://test.authority/aad.ropc";

    @Test
    public void test2() throws InterruptedException, PackageManager.NameNotFoundException, MsalException {
        new AcquireTokenBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity,
                                      final CountDownLatch latch) {
                AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withScopes(Arrays.asList(SCOPES))
                        .fromAuthority(AAD_ROPC_TEST_AUTHORITY)
                        .callback(new AuthenticationCallback() {
                            @Override
                            public void onSuccess(IAuthenticationResult authenticationResult) {
                                Assert.assertTrue(!StringUtil.isEmpty(authenticationResult.getAccessToken()));
                                latch.countDown();
                            }

                            @Override
                            public void onError(MsalException exception) {
                                fail(exception.getMessage());
                            }

                            @Override
                            public void onCancel() {
                                fail("User cancelled flow");
                            }
                        })
                        .build();

                publicClientApplication.acquireToken(parameters);
            }

        }.performTest();
    }

}
