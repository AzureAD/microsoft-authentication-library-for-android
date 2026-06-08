package com.microsoft.identity.client.e2e.tests.network;

import com.microsoft.identity.client.MultiTenantAccount;
import com.microsoft.identity.client.e2e.rules.NetworkTestsRuleChain;
import com.microsoft.identity.client.e2e.shadows.ShadowAuthority;
import com.microsoft.identity.client.e2e.shadows.ShadowAndroidSdkStorageEncryptionManager;
import com.microsoft.identity.client.e2e.tests.AcquireTokenAbstractTest;
import com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper;
import com.microsoft.identity.labapi.utilities.BuildConfig;
import com.microsoft.identity.labapi.utilities.authentication.LabApiAuthenticationClient;
import com.microsoft.identity.labapi.utilities.client.ILabAccount;
import com.microsoft.identity.labapi.utilities.client.LabClient;
import com.microsoft.identity.labapi.utilities.constants.UserType;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

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
@Config(shadows = {ShadowAndroidSdkStorageEncryptionManager.class, ShadowAuthority.class})
public class GuestAccountAcquireTokenNetworkTests extends AcquireTokenAbstractTest {

    @Rule
    public TestRule rule = NetworkTestsRuleChain.getRule();

    private final LabClient labClient = new LabClient(new LabApiAuthenticationClient(
            BuildConfig.LAB_CLIENT_SECRET
    ));

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
    public void testGetTokenSilentlyForGuestTenantSuccess() throws LabApiException {
        final String authorityPrefix = "https://login.microsoftonline.com/";

        final ILabAccount labGuest = labClient.getAccountFromLabJsonStringInMobileBuildVault(UserType.GUEST);

        // get a token interactively for home tenant
        performInteractiveAcquireTokenCall(labGuest.getUsername());

        // get token silently for home tenant
        performSilentAcquireTokenCall(getAccount(), authorityPrefix + labGuest.getHomeTenantId());

        // just making sure that it is indeed guest tenant by comparing against home tenant
        Assert.assertNotSame(labGuest.getHomeTenantId(), labGuest.getGuestTenantId());
        // create authority from guest tenant id and use to obtain a token silently for guest tenant
        performSilentAcquireTokenCall(getAccount(), authorityPrefix + labGuest.getGuestTenantId());


        Assert.assertTrue(getAccount() instanceof MultiTenantAccount);

        final MultiTenantAccount multiTenantAccount = (MultiTenantAccount) getAccount();


        // make sure that we have a tenant profile for the guest tenant
        Assert.assertTrue(multiTenantAccount.getTenantProfiles().containsKey(labGuest.getGuestTenantId()));
    }

    @Test
    public void testGuestSignInDirectlyIntoGuestTenantSuccess() throws LabApiException {
        final String authorityPrefix = "https://login.microsoftonline.com/";

        final ILabAccount labGuest = labClient.getAccountFromLabJsonStringInMobileBuildVault(UserType.GUEST);

        // just making sure that it is indeed guest tenant by comparing against home tenant
        Assert.assertNotSame(labGuest.getHomeTenantId(), labGuest.getGuestTenantId());
        // create authority from guest tenant id and use to obtain a token interactively for guest tenant
        performInteractiveAcquireTokenCall(labGuest.getUsername(), authorityPrefix + labGuest.getGuestTenantId());
        // create authority from guest tenant id and use to obtain a token silently for guest tenant
        performSilentAcquireTokenCall(getAccount(), authorityPrefix + labGuest.getGuestTenantId());

        Assert.assertTrue(getAccount() instanceof MultiTenantAccount);

        MultiTenantAccount multiTenantAccount = (MultiTenantAccount) getAccount();

        // we should NOT have claims for root account as we didn't acquire a token for it
        Assert.assertNull(multiTenantAccount.getClaims());

        // make sure that we have a tenant profile for the guest tenant
        Assert.assertTrue(multiTenantAccount.getTenantProfiles().containsKey(labGuest.getGuestTenantId()));

        // now get a token silently for home tenant
        performSilentAcquireTokenCall(multiTenantAccount, authorityPrefix + labGuest.getHomeTenantId());

        multiTenantAccount = (MultiTenantAccount) getAccount();

        // we should now have claims for root account as we just acquired a token for it
        Assert.assertNotNull(multiTenantAccount.getClaims());

    }
}
