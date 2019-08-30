package com.microsoft.identity.client.robolectric.tests;

import android.app.Activity;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.msal.R;

import org.mockito.Mockito;

import java.io.File;

public abstract class AcquireTokenBaseTest {

    private static final String CONFIG_FILE_PATH = "src/test/res/raw/aad_test_config.json";

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

        File configFile = new File(CONFIG_FILE_PATH);

        PublicClientApplication.create(context, configFile, new PublicClientApplication.ApplicationCreatedListener() {
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
