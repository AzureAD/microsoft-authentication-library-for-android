package com.microsoft.identity.client.roboelectric;

import android.app.Activity;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.msal.R;

import org.mockito.Mockito;

public abstract class PublicClientApplicationBaseTest {

    abstract void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                       final Activity activity) throws InterruptedException;

    private Activity getActivity(final Context context) {
        final Activity mockedActivity = Mockito.mock(Activity.class);
        Mockito.when(mockedActivity.getApplicationContext()).thenReturn(context);

        return mockedActivity;
    }


    public void performTest() {
        final Context context = ApplicationProvider.getApplicationContext();
        final Activity testActivity = getActivity(context);

        PublicClientApplication.create(context, R.raw.test_msal_config_multiple_account, new PublicClientApplication.ApplicationCreatedListener() {
            @Override
            public void onCreated(IPublicClientApplication application) {
                try {
                    makeAcquireTokenCall(application, testActivity);
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
