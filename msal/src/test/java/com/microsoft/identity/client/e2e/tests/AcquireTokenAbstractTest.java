package com.microsoft.identity.client.e2e.tests;

import org.junit.Before;

public abstract class AcquireTokenAbstractTest extends PublicClientApplicationAbstractTest implements IAcquireTokenTest {

    protected String[] mScopes;

    @Before
    public void setup() {
        mScopes = getScopes();
        super.setup();
    }
}
