package com.microsoft.identity.client;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link AuthenticationActivity}.
 */
@RunWith(AndroidJUnit4.class)
public final class AuthenticationActivityTest {
    private static final int REQUEST_ID = 1234;
    private Context mAppContext;
    private String mRedirectUri;

    private final ActivityTestRule mTestActivityRule = new ActivityTestRule<>(TestActivity.class,
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

        mTestActivityRule.launchActivity(TestActivity.createIntent(mAppContext, intent));

        Assert.assertTrue(TestActivity.getResultCode() == Constants.UIResponse.AUTH_CODE_ERROR);
        final Intent resultData = TestActivity.getResultData();
        Assert.assertNotNull(resultData);
        Assert.assertTrue(resultData.getStringExtra(Constants.UIResponse.ERROR_CODE).equals(
                MsalClientException.UNRESOLVABLE_INTENT));
        Assert.assertTrue(resultData.getStringExtra(Constants.UIResponse.ERROR_DESCRIPTION).contains("Request url"));
    }

    private String getRequestUri() {
        return mRedirectUri + "?grant_type=code&";
    }
}
