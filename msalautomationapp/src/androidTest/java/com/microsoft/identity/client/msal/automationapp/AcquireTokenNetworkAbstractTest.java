package com.microsoft.identity.client.msal.automationapp;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.ui.automation.app.IApp;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
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
        final String tempUserType = getTempUserType();

        if (query != null) {
            mLoginHint = mUsername = LabUserHelper.loadUserForTest(query);
        } else if (tempUserType != null) {
            mLoginHint = mUsername = LabUserHelper.loadTempUser(tempUserType);
            try {
                // temp user takes some time to actually being created even though it may be
                // returned by the LAB API. Adding a wait here before we proceed with the test.
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            throw new IllegalArgumentException("Both Lab User query and temp user type were null.");
        }

        super.setup();

        if (mBroker != null) {
            mBroker.install();
        }
    }

    @Override
    public IApp getBrowser() {
        return new BrowserChrome();
    }

}
