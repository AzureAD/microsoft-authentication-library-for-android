package com.microsoft.identity.client.msal.automationapp;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Before;

public abstract class AcquireTokenNetworkAbstractTest extends AcquireTokenAbstractTest implements IAcquireTokenNetworkTest {

    protected String mUsername;
    protected String mLoginHint;

    @Before
    public void setup() {
        System.setProperty(
                "org.mockito.android.target",
                ApplicationProvider
                        .getApplicationContext()
                        .getCacheDir()
                        .getPath()
        );

        AcquireTokenTestHelper.setAccount(null);
        final LabUserQuery query = getLabUserQuery();
        mLoginHint = mUsername = LabUserHelper.loadUserForTest(query);
        super.setup();
    }
}
