package com.microsoft.identity.client.msal.automationapp;

import android.app.Activity;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.rule.ActivityTestRule;

import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import static org.junit.Assert.fail;

public abstract class PublicClientApplicationAbstractTest implements IPublicClientApplicationTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule(MainActivity.class);

    protected final String SHARED_PREFERENCES_NAME = "com.microsoft.identity.client.account_credential_cache";

    protected Context mContext;
    protected Activity mActivity;
    protected IPublicClientApplication mApplication;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        //mActivity = TestUtils.getMockActivity(mContext);
        //mActivity = new MainActivity();
        mActivity = mActivityRule.getActivity();
        setupPCA();
    }

    @After
    public void cleanup() {
        mActivity.finish();
    }

    private void setupPCA() {
        try {
            mApplication = PublicClientApplication.create(mContext, getConfigFileResourceId());
        } catch (InterruptedException e) {
            fail(e.getMessage());
        } catch (MsalException e) {
            fail(e.getMessage());
        }
    }
}
