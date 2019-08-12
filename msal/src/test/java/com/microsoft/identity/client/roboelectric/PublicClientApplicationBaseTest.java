package com.microsoft.identity.client.roboelectric;

import android.app.Activity;
import android.content.Context;

import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.msal.R;

import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.Scheduler;

public abstract class PublicClientApplicationBaseTest {

    abstract void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                       final Activity activity) throws InterruptedException;

    private Activity getActivity(final Context context) {
        final Activity mockedActivity = Mockito.mock(Activity.class);
        Mockito.when(mockedActivity.getApplicationContext()).thenReturn(context);

        return mockedActivity;
    }

    private void flushScheduler() {
//        Scheduler scheduler = ShadowLooper.shadowMainLooper().getScheduler();
        Scheduler scheduler = ShadowLooper.getShadowMainLooper().getScheduler();
        while (!scheduler.advanceToLastPostedRunnable()) ;
    }

    public void performTest() {
        //HttpWebRequest.disableNetworkCheckForTestForTest(true);
        final Context context = RuntimeEnvironment.application;
        final Activity testActivity = getActivity(context);

        PublicClientApplication.create(context, R.raw.test_msal_config_multiple_account, new PublicClientApplication.ApplicationCreatedListener() {
            @Override
            public void onCreated(IPublicClientApplication application) {
                try {
                    makeAcquireTokenCall(application, testActivity);
                    flushScheduler();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(MsalException exception) {
                exception.printStackTrace();
            }
        });
    }


}
