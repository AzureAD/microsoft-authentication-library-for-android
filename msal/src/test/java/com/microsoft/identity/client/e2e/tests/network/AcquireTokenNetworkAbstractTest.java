package com.microsoft.identity.client.e2e.tests.network;

import com.microsoft.identity.client.e2e.shadows.ShadowAuthority;
import com.microsoft.identity.client.e2e.shadows.ShadowMsalUtils;
import com.microsoft.identity.client.e2e.shadows.ShadowStorageHelper;
import com.microsoft.identity.client.e2e.tests.AcquireTokenAbstractTest;
import com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowStorageHelper.class, ShadowAuthority.class, ShadowMsalUtils.class})
public abstract class AcquireTokenNetworkAbstractTest extends AcquireTokenAbstractTest implements IAcquireTokenNetworkTest {

    String mUsername;

    @Before
    public void setup() {
        AcquireTokenTestHelper.setAccount(null);
        final LabUserQuery query = getLabUserQuery();
        mUsername = LabUserHelper.loadUserForTest(query);
        super.setup();
    }
}
