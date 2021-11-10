package com.microsoft.identity.client.e2e.tests.network;

import com.microsoft.identity.client.MultiTenantAccount;
import com.microsoft.identity.client.e2e.rules.NetworkTestsRuleChain;
import com.microsoft.identity.client.e2e.shadows.ShadowAuthority;
import com.microsoft.identity.client.e2e.shadows.ShadowPublicClientApplicationConfiguration;
import com.microsoft.identity.client.e2e.shadows.ShadowAndroidSdkStorageEncryptionManager;
import com.microsoft.identity.client.e2e.tests.AcquireTokenAbstractTest;
import com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabGuest;
import com.microsoft.identity.internal.testutils.labutils.LabGuestAccountHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper.getAccount;
import static com.microsoft.identity.internal.testutils.TestConstants.Configurations.MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH;
import static com.microsoft.identity.internal.testutils.TestConstants.Scopes.USER_READ_SCOPE;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAndroidSdkStorageEncryptionManager.class, ShadowAuthority.class, ShadowPublicClientApplicationConfiguration.class})
public class GuestAccountAcquireTokenNetworkTests extends AcquireTokenAbstractTest {

    @Rule
    public TestRule rule = NetworkTestsRuleChain.getRule();

    @Override
    public String[] getScopes() {
        return USER_READ_SCOPE;
    }

    @Override
    public String getAuthority() {
        return AcquireTokenTestHelper.getAccount().getAuthority();
    }

    @Override
    public String getConfigFilePath() {
        return MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH;
    }

    @Test // test that we can use mrrt to get a token silently for guest accounts
    public void testGetTokenSilentlyForEachGuestTenantSuccess() {
        final String authorityPrefix = "https://login.microsoftonline.com/";

        final LabUserQuery query = new LabUserQuery();
        query.userType = LabConstants.UserType.GUEST;
        query.guestHomedIn = LabConstants.GuestHomedIn.HOST_AZURE_AD;

        final LabGuest labGuest = LabGuestAccountHelper.loadGuestAccountFromLab(query);

        // sanity check - make sure that lab api provided a guest account that is part of at least
        // one guest tenant
        Assert.assertTrue(labGuest.getGuestLabTenants() != null && labGuest.getGuestLabTenants().size() > 0);

        // get a token interactively for home tenant
        performInteractiveAcquireTokenCall(labGuest.getHomeUpn());

        // get token silently for home tenant
        performSilentAcquireTokenCall(getAccount(), authorityPrefix + labGuest.getHomeTenantId());

        // get a token silently for each guest tenant
        for (String guestLabTenant : labGuest.getGuestLabTenants()) {
            // just making sure that it is indeed guest tenant by comparing against home tenant
            Assert.assertNotSame(labGuest.getHomeTenantId(), guestLabTenant);
            // create authority from guest tenant id and use to obtain a token silently for guest tenant
            performSilentAcquireTokenCall(getAccount(), authorityPrefix + guestLabTenant);
        }

        Assert.assertTrue(getAccount() instanceof MultiTenantAccount);

        final MultiTenantAccount multiTenantAccount = (MultiTenantAccount) getAccount();

        // we should have as many tenant profiles as the amount of guest tenants we requested tokens for
        Assert.assertSame(labGuest.getGuestLabTenants().size(), multiTenantAccount.getTenantProfiles().size());

        // make sure that we have a tenant profile for each guest tenant
        for (String guestLabTenant : labGuest.getGuestLabTenants()) {
            Assert.assertTrue(multiTenantAccount.getTenantProfiles().containsKey(guestLabTenant));
        }
    }

    @Test
    public void testGuestSignInDirectlyIntoGuestTenantSuccess() {
        final String authorityPrefix = "https://login.microsoftonline.com/";

        final LabUserQuery query = new LabUserQuery();
        query.userType = LabConstants.UserType.GUEST;
        query.guestHomedIn = LabConstants.GuestHomedIn.HOST_AZURE_AD;

        final LabGuest labGuest = LabGuestAccountHelper.loadGuestAccountFromLab(query);

        // sanity check - make sure that lab api provided a guest account that is part of at least
        // one guest tenant
        Assert.assertTrue(labGuest.getGuestLabTenants() != null && labGuest.getGuestLabTenants().size() > 0);

        // get a token interactively for each guest tenant
        for (String guestLabTenant : labGuest.getGuestLabTenants()) {
            // just making sure that it is indeed guest tenant by comparing against home tenant
            Assert.assertNotSame(labGuest.getHomeTenantId(), guestLabTenant);
            // create authority from guest tenant id and use to obtain a token interactively for guest tenant
            performInteractiveAcquireTokenCall(labGuest.getHomeUpn(), authorityPrefix + guestLabTenant);
        }

        Assert.assertTrue(getAccount() instanceof MultiTenantAccount);

        MultiTenantAccount multiTenantAccount = (MultiTenantAccount) getAccount();

        // we should NOT have claims for root account as we didn't acquire a token for it
        Assert.assertNull(multiTenantAccount.getClaims());

        // we should have as many tenant profiles as the amount of guest tenants we requested tokens for
        Assert.assertSame(labGuest.getGuestLabTenants().size(), multiTenantAccount.getTenantProfiles().size());

        // make sure that we have a tenant profile for each guest tenant
        for (String guestLabTenant : labGuest.getGuestLabTenants()) {
            Assert.assertTrue(multiTenantAccount.getTenantProfiles().containsKey(guestLabTenant));
        }

        // now get a token silently for home tenant
        performSilentAcquireTokenCall(multiTenantAccount, authorityPrefix + labGuest.getHomeTenantId());

        multiTenantAccount = (MultiTenantAccount) getAccount();

        // we should now have claims for root account as we just acquired a token for it
        Assert.assertNotNull(multiTenantAccount.getClaims());

    }
}
