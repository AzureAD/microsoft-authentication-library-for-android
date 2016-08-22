package com.microsoft.identity.client;

import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Tests for {@link AuthenticationActivity}.
 */
@RunWith(AndroidJUnit4.class)
public final class AuthenticationActivityTest {
    private static int REQUEST_ID = 1234;
    private Context mAppContext;
    private String mRedirectUri;

    public ActivityTestRule mTestActivityRule = new ActivityTestRule<>(TestActivity.class,
            true, false);
    @Before
    public void setUp() {
        mAppContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mRedirectUri = "msauth-client-id://" + mAppContext.getPackageName();
    }

    @Test
    public void testIntentWithNoRequestUri() {
        final Intent intent = new Intent(mAppContext, AuthenticationActivity.class);
        intent.putExtra(Constants.REQUEST_ID, REQUEST_ID);
        intent.putExtra(Constants.REDIRECT_INTENT, mRedirectUri);
        mTestActivityRule.launchActivity(TestActivity.createIntent(mAppContext, intent));

        Assert.assertTrue(TestActivity.getResultCode() == Constants.UIResponse.AUTH_CODE_ERROR);
        final Intent resultData = TestActivity.getResultData();
        Assert.assertNotNull(resultData);
        Assert.assertTrue(resultData.getStringExtra(Constants.UIResponse.ERROR_CODE).equals(
                Constants.MSALError.INVALID_REQUEST));
        Assert.assertTrue(resultData.getStringExtra(Constants.UIResponse.ERROR_DESCRIPTION).contains("Request url"));
    }

    private String getRequestUri() {
        return mRedirectUri + "?grant_type=code&";
    }
}
