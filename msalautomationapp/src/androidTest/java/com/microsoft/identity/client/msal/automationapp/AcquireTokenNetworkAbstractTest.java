package com.microsoft.identity.client.msal.automationapp;

import com.microsoft.identity.client.ui.automation.app.IApp;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Before;

public abstract class AcquireTokenNetworkAbstractTest extends AcquireTokenAbstractTest implements IAcquireTokenNetworkTest {

    protected String mUsername;
    protected String mLoginHint;

    public static final int TEMP_USER_WAIT_TIME = 15000;

    @Before
    public void setup() {
        final LabUserQuery query = getLabUserQuery();
        final String tempUserType = getTempUserType();

        if (query != null) {
            mLoginHint = mUsername = LabUserHelper.loadUserForTest(query);
        } else if (tempUserType != null) {
            mLoginHint = mUsername = LabUserHelper.loadTempUser(tempUserType);
            try {
                // temp user takes some time to actually being created even though it may be
                // returned by the LAB API. Adding a wait here before we proceed with the test.
                Thread.sleep(TEMP_USER_WAIT_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            throw new IllegalArgumentException("Both Lab User query and temp user type were null.");
        }

        super.setup();
    }

    @Override
    public IApp getBrowser() {
        return new BrowserChrome();
    }

}
