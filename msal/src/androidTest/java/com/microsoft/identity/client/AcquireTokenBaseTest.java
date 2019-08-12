package com.microsoft.identity.client;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.support.test.InstrumentationRegistry;

import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.adal.internal.net.HttpWebRequest;
import com.microsoft.identity.msal.test.R;

import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;

public abstract class AcquireTokenBaseTest {

    private Context mAppContext;

    abstract void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                       final Activity activity,
                                       final CountDownLatch releaseLock) throws InterruptedException;

    private Activity getActivity(final Context context) {
        final Activity mockedActivity = Mockito.mock(Activity.class);
        Mockito.when(mockedActivity.getApplicationContext()).thenReturn(context);

        return mockedActivity;
    }

    private void mockPackageManagerWithClientId(final Context context)
            throws PackageManager.NameNotFoundException {
        final PackageManager mockedPackageManager = context.getPackageManager();
        final ApplicationInfo mockedApplicationInfo = Mockito.mock(ApplicationInfo.class);

        Mockito.when(mockedPackageManager.getApplicationInfo(Mockito.anyString(), Mockito.anyInt())).thenReturn(mockedApplicationInfo);

        final PackageInfo mockedPackageInfo = Mockito.mock(PackageInfo.class);
        Mockito.when(mockedPackageManager.getPackageInfo(Mockito.anyString(), Mockito.anyInt())).thenReturn(mockedPackageInfo);
    }

    private Context createMockedContext() throws PackageManager.NameNotFoundException {
        final Context context = new MockContext(mAppContext);
        mockPackageManagerWithClientId(context);
        return context;
    }

    public void performTest() throws InterruptedException, PackageManager.NameNotFoundException, MsalException {
        HttpWebRequest.disableNetworkCheckForTestForTest(true);
        mAppContext = InstrumentationRegistry.getContext().getApplicationContext();
        final Context context = createMockedContext();

        final CountDownLatch latch = new CountDownLatch(1);
        final Activity testActivity = getActivity(context);

        final IPublicClientApplication application = PublicClientApplication.create(context, R.raw.test_msal_config_multiple_account);
        makeAcquireTokenCall(application, testActivity, latch);
        latch.await();
    }

    private static class MockContext extends ContextWrapper {
        private final PackageManager mPackageManager;
        private final ConnectivityManager mConnectivityManager;

        MockContext(final Context context) {
            super(context);
            mPackageManager = Mockito.mock(PackageManager.class);
            mConnectivityManager = Mockito.mock(ConnectivityManager.class);
        }

        @Override
        public PackageManager getPackageManager() {
            return mPackageManager;
        }

        @Override
        public Object getSystemService(String name) {
            if (Context.CONNECTIVITY_SERVICE.equals(name)) {
                return mConnectivityManager;
            }

            return super.getSystemService(name);
        }
    }


}
