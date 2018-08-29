package com.microsoft.identity.client;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.microsoft.identity.client.authorities.AccountsInOneOrganization;
import com.microsoft.identity.client.authorities.ActiveDirectoryFederationServicesAuthority;
import com.microsoft.identity.client.authorities.AllAccounts;
import com.microsoft.identity.client.authorities.AnyOrganizationalAccount;
import com.microsoft.identity.client.authorities.AnyPersonalAccount;
import com.microsoft.identity.client.authorities.Authority;
import com.microsoft.identity.client.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.client.authorities.AzureActiveDirectoryB2CAuthority;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.msal.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PublicClientConfigurationTest {

    private Context mContext;
    private PublicClientApplicationConfiguration mDefaultConfig;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getContext().getApplicationContext();
        mDefaultConfig = loadConfig(R.raw.msal_default_config);
    }

    /**
     * Test that the default config loads successfully.
     */
    @Test
    public void testDefaultConfigurationLoads() {
        assertNotNull(mDefaultConfig);
        assertNotNull(mDefaultConfig.mAuthorities);
        assertNotNull(mDefaultConfig.mAuthorizationAgent);
        assertNotNull(mDefaultConfig.mHttpConfiguration);
    }

    /**
     * Test that the min configuration loads.
     */
    @Test
    public void testMinimumConfigurationLoads() {
        final PublicClientApplicationConfiguration minConfig = loadConfig(R.raw.test_pcaconfig_min);
        assertNotNull(minConfig.mClientId);
        assertNotNull(minConfig.mRedirectUri);
    }

    /**
     * Verifies that a minimum configuration containing only:
     * client_id
     * redirect_uri
     * <p>
     * Is merged with the default configuration correctly.
     */
    @Test
    public void testMinimumValidConfigurationMerge() {
        // Record the values of the default config to verify the merge action
        final List<Authority> authorities = mDefaultConfig.mAuthorities;
        final AuthorizationAgent agent = mDefaultConfig.mAuthorizationAgent;
        final HttpConfiguration httpConfiguration = mDefaultConfig.mHttpConfiguration;

        // Load the min config
        final PublicClientApplicationConfiguration minConfig = loadConfig(R.raw.test_pcaconfig_min);

        // Merge it
        mDefaultConfig.mergeConfiguration(minConfig);

        // Assert that the values have merged successfully
        assertEquals(authorities, mDefaultConfig.mAuthorities);
        assertEquals(agent, mDefaultConfig.mAuthorizationAgent);
        assertEquals(httpConfiguration, mDefaultConfig.mHttpConfiguration);
        assertEquals(minConfig.mClientId, mDefaultConfig.mClientId);
        assertEquals(minConfig.mRedirectUri, mDefaultConfig.mRedirectUri);
    }

    /**
     * Verify B2C Authority set via configuration correctly.
     */
    @Test
    public void testB2CAuthorityValidConfiguration() {
        final PublicClientApplicationConfiguration b2cConfig = loadConfig(R.raw.test_pcaconfig_b2c);
        assertNotNull(b2cConfig);
        assertNotNull(b2cConfig.mClientId);
        assertNotNull(b2cConfig.mRedirectUri);
        assertNotNull(b2cConfig.mAuthorities);
        assertEquals(1, b2cConfig.mAuthorities.size());

        // Grab the Authority from the config
        final Authority config = b2cConfig.mAuthorities.get(0);

        // Test that it is a B2C Authority.
        assertTrue(config instanceof AzureActiveDirectoryB2CAuthority);
        assertNotNull(config.getAuthorityUri());
    }

    /**
     * Verify ADFS Authority set via configuration correctly.
     */
    @Test
    public void testADFSAuthorityValidConfiguration() {
        final PublicClientApplicationConfiguration adfsConfig = loadConfig(R.raw.test_pcaconfig_adfs);
        assertNotNull(adfsConfig);
        assertNotNull(adfsConfig.mClientId);
        assertNotNull(adfsConfig.mRedirectUri);
        assertNotNull(adfsConfig.mAuthorities);
        assertEquals(1, adfsConfig.mAuthorities.size());

        // Grab the Authority from the config
        final Authority config = adfsConfig.mAuthorities.get(0);

        // Test that it is a B2C Authority.
        assertTrue(config instanceof ActiveDirectoryFederationServicesAuthority);
        assertNotNull(config.getAuthorityUri());
    }

    /**
     * Verify audience type of AccountsInOneOrganization can be set.
     */
    @Test
    public void testAudienceAccountsInOneOrganziation() {
        final PublicClientApplicationConfiguration config = loadConfig(R.raw.test_pcaconfig_aud_accountsinoneorg);
        assertNotNull(config);
        assertEquals(1, config.mAuthorities.size());
        final Authority authority = config.mAuthorities.get(0);
        assertTrue(authority instanceof AzureActiveDirectoryAuthority);
        final AzureActiveDirectoryAuthority azureActiveDirectoryAuthority = (AzureActiveDirectoryAuthority) authority;
        assertTrue(azureActiveDirectoryAuthority.mAudience instanceof AccountsInOneOrganization);
    }

    /**
     * Verify audience type of AnyOrganizationAccount can be set.
     */
    @Test
    public void testAudienceAnyOrganizationAccount() {
        final PublicClientApplicationConfiguration config = loadConfig(R.raw.test_pcaconfig_aud_anyorg);
        assertNotNull(config);
        assertEquals(1, config.mAuthorities.size());
        final Authority authority = config.mAuthorities.get(0);
        assertTrue(authority instanceof AzureActiveDirectoryAuthority);
        final AzureActiveDirectoryAuthority azureActiveDirectoryAuthority = (AzureActiveDirectoryAuthority) authority;
        assertTrue(azureActiveDirectoryAuthority.mAudience instanceof AnyOrganizationalAccount);
    }

    /**
     * Verify audience type of AllAccounts can be set.
     */
    @Test
    public void testAudienceAllAccounts() {
        final PublicClientApplicationConfiguration config = loadConfig(R.raw.test_pcaconfig_aud_allaccts);
        assertNotNull(config);
        assertEquals(1, config.mAuthorities.size());
        final Authority authority = config.mAuthorities.get(0);
        assertTrue(authority instanceof AzureActiveDirectoryAuthority);
        final AzureActiveDirectoryAuthority azureActiveDirectoryAuthority = (AzureActiveDirectoryAuthority) authority;
        assertTrue(azureActiveDirectoryAuthority.mAudience instanceof AllAccounts);
    }

    /**
     * Verify audience type of AnyPersonalAccount can be set.
     */
    @Test
    public void testAudienceAnyPersonalAccount() {
        final PublicClientApplicationConfiguration config = loadConfig(R.raw.test_pcaconfig_aud_anypersonal);
        assertNotNull(config);
        assertEquals(1, config.mAuthorities.size());
        final Authority authority = config.mAuthorities.get(0);
        assertTrue(authority instanceof AzureActiveDirectoryAuthority);
        final AzureActiveDirectoryAuthority azureActiveDirectoryAuthority = (AzureActiveDirectoryAuthority) authority;
        assertTrue(azureActiveDirectoryAuthority.mAudience instanceof AnyPersonalAccount);
    }

    /**
     * Tests that slice parameters can be set.
     */
    @Test
    public void testSliceParametersSet() {
        final PublicClientApplicationConfiguration config = loadConfig(R.raw.test_pcaconfig_with_slice);
        assertNotNull(config);
        assertEquals(1, config.mAuthorities.size());
        final Authority authority = config.mAuthorities.get(0);
        final AzureActiveDirectoryAuthority azureActiveDirectoryAuthority = (AzureActiveDirectoryAuthority) authority;
        assertNotNull(azureActiveDirectoryAuthority.mSlice.getDC());
        assertNotNull(azureActiveDirectoryAuthority.mSlice.getSlice());
    }

    /**
     * Tests that flight parameters can be set.
     */
    @Test
    public void testFlightParametersSet() {
        final PublicClientApplicationConfiguration config = loadConfig(R.raw.test_pcaconfig_with_flight);
        assertNotNull(config);
        assertEquals(1, config.mAuthorities.size());
        final Authority authority = config.mAuthorities.get(0);
        final AzureActiveDirectoryAuthority azureActiveDirectoryAuthority = (AzureActiveDirectoryAuthority) authority;
        assertFalse(azureActiveDirectoryAuthority.mFlightParameters.isEmpty());
    }

    /**
     * Verify that unknown authority type results in exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUnknownAuthorityException() {
        final PublicClientApplicationConfiguration b2cConfig = loadConfig(R.raw.test_pcaconfig_unknown);
        b2cConfig.validateConfiguration();
    }

    /**
     * Verify that unknown audience type results in exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUnknownAudienceException() {
        final PublicClientApplicationConfiguration configWithInvalidAudience = loadConfig(R.raw.test_pcaconfig_unknown_audience);
        assertNotNull(configWithInvalidAudience);
        assertFalse(configWithInvalidAudience.mAuthorities.isEmpty());

        final Authority authorityWithInvalidAudience = configWithInvalidAudience.mAuthorities.get(0);
        assertNotNull(authorityWithInvalidAudience);
        assertTrue(authorityWithInvalidAudience instanceof AzureActiveDirectoryAuthority);
        configWithInvalidAudience.validateConfiguration();
    }

    /**
     * Verify that null client id throws an exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNullClientIdException() {
        final PublicClientApplicationConfiguration configWithoutClientId = loadConfig(R.raw.test_pcaconfig_missing_clientid);
        configWithoutClientId.validateConfiguration();
    }

    /**
     * Verify that null redirect URI throws an exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNullRedirectUrlException() {
        final PublicClientApplicationConfiguration configWithoutRedirect = loadConfig(R.raw.test_pcaconfig_missing_redirect);
        configWithoutRedirect.validateConfiguration();
    }

    private PublicClientApplicationConfiguration loadConfig(final int resourceId) {
        return PublicClientApplication.loadConfiguration(mContext, resourceId);
    }

}
