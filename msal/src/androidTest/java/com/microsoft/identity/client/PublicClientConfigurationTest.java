//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.client.configuration.HttpConfiguration;
import com.microsoft.identity.common.java.authorities.AccountsInOneOrganization;
import com.microsoft.identity.common.java.authorities.ActiveDirectoryFederationServicesAuthority;
import com.microsoft.identity.common.java.authorities.AllAccounts;
import com.microsoft.identity.common.java.authorities.AnyOrganizationalAccount;
import com.microsoft.identity.common.java.authorities.AnyPersonalAccount;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryB2CAuthority;
import com.microsoft.identity.common.java.ui.AuthorizationAgent;
import com.microsoft.identity.msal.test.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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

    @After
    public void tearDown() {
        File testConfigFile = getTestConfigFile();
        if (testConfigFile.exists()) {
            assertTrue(testConfigFile.delete());
        }
    }

    /**
     * Test that the default config loads successfully.
     */
    @Test
    public void testDefaultConfigurationLoads() {
        assertNotNull(mDefaultConfig);
        assertNotNull(mDefaultConfig.getAuthorities());
        assertNotNull(mDefaultConfig.getAuthorizationAgent());
        assertNotNull(mDefaultConfig.getHttpConfiguration());
    }

    /**
     * Tests that Http settings can be configured for timeouts.
     */
    @Test
    public void testDefaultConfigurationLoadsWithHttpConfig() {
        assertNotNull(mDefaultConfig.getHttpConfiguration());
        final HttpConfiguration httpConfiguration = mDefaultConfig.getHttpConfiguration();
        assertTrue(httpConfiguration.getConnectTimeout() >= 1);
        assertTrue(httpConfiguration.getReadTimeout() >= 1);
    }

    /**
     * Test that the min configuration loads.
     */
    @Test
    public void testMinimumConfigurationLoads() {
        final PublicClientApplicationConfiguration minConfig = loadConfig(R.raw.test_pcaconfig_min);
        assertNotNull(minConfig.getClientId());
        assertNotNull(minConfig.getRedirectUri());
    }

    /**
     * Verifies that a minimum configuration containing only:
     * client_id
     * redirect_uri
     * <p>
     * Is merged with the default configuration correctly.
     */
    private void testMinimumValidConfigurationMerge(PublicClientApplicationConfiguration minConfig) {
        // Record the values of the default config to verify the merge action
        final List<Authority> authorities = mDefaultConfig.getAuthorities();
        final AuthorizationAgent agent = mDefaultConfig.getAuthorizationAgent();
        final HttpConfiguration httpConfiguration = mDefaultConfig.getHttpConfiguration();

        // Merge it
        mDefaultConfig.mergeConfiguration(minConfig);

        // Assert that the values have merged successfully
        assertEquals(authorities, mDefaultConfig.getAuthorities());
        assertEquals(agent, mDefaultConfig.getAuthorizationAgent());
        assertEquals(httpConfiguration, mDefaultConfig.getHttpConfiguration());
        assertEquals(minConfig.getClientId(), mDefaultConfig.getClientId());
        assertEquals(minConfig.getRedirectUri(), mDefaultConfig.getRedirectUri());
    }

    @Test
    public void testMinimumValidConfigurationMergeViaResource() {
        testMinimumValidConfigurationMerge(loadConfig(R.raw.test_pcaconfig_min));
    }

    @Test
    public void testMinimumValidConfigurationMergeViaFile() throws IOException {
        final File file = copyResourceToTestFile(R.raw.test_pcaconfig_min);
        testMinimumValidConfigurationMerge(PublicClientApplicationConfigurationFactory.loadConfiguration(file));
    }

    /**
     * Verify B2C Authority set via configuration correctly.
     */
    private void testB2CAuthorityValidConfiguration(PublicClientApplicationConfiguration b2cConfig) {
        assertNotNull(b2cConfig);
        assertNotNull(b2cConfig.getClientId());
        assertNotNull(b2cConfig.getRedirectUri());
        assertNotNull(b2cConfig.getAuthorities());
        assertEquals(1, b2cConfig.getAuthorities().size());

        // Grab the Authority from the config
        final Authority config = b2cConfig.getAuthorities().get(0);

        // Test that it is a B2C Authority.
        assertTrue(config instanceof AzureActiveDirectoryB2CAuthority);
        assertNotNull(config.getAuthorityUri());
    }

    @Test
    public void testB2CAuthorityValidConfigurationViaResource() {
        final PublicClientApplicationConfiguration b2cConfig = loadConfig(R.raw.test_pcaconfig_b2c);
        testB2CAuthorityValidConfiguration(b2cConfig);
    }

    @Test
    public void testB2CAuthorityValidConfigurationViaFile() throws IOException {
        final File file = copyResourceToTestFile(R.raw.test_pcaconfig_b2c);
        final PublicClientApplicationConfiguration b2cConfig = loadConfig(file);
        testB2CAuthorityValidConfiguration(b2cConfig);
    }

    /**
     * Verify ADFS Authority set via configuration correctly.
     */
    @Test
    public void testADFSAuthorityValidConfiguration() {
        final PublicClientApplicationConfiguration adfsConfig = loadConfig(R.raw.test_pcaconfig_adfs);
        assertNotNull(adfsConfig);
        assertNotNull(adfsConfig.getClientId());
        assertNotNull(adfsConfig.getRedirectUri());
        assertNotNull(adfsConfig.getAuthorities());
        assertEquals(1, adfsConfig.getAuthorities().size());

        // Grab the Authority from the config
        final Authority config = adfsConfig.getAuthorities().get(0);

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
        assertEquals(1, config.getAuthorities().size());
        final Authority authority = config.getAuthorities().get(0);
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
        assertEquals(1, config.getAuthorities().size());
        final Authority authority = config.getAuthorities().get(0);
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
        assertEquals(1, config.getAuthorities().size());
        final Authority authority = config.getAuthorities().get(0);
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
        assertEquals(1, config.getAuthorities().size());
        final Authority authority = config.getAuthorities().get(0);
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
        assertEquals(1, config.getAuthorities().size());
        final Authority authority = config.getAuthorities().get(0);
        final AzureActiveDirectoryAuthority azureActiveDirectoryAuthority = (AzureActiveDirectoryAuthority) authority;
        assertNotNull(azureActiveDirectoryAuthority.mSlice.getDataCenter());
        assertNotNull(azureActiveDirectoryAuthority.mSlice.getSlice());
    }

    /**
     * Tests that flight parameters can be set.
     */
    @Test
    public void testFlightParametersSet() {
        final PublicClientApplicationConfiguration config = loadConfig(R.raw.test_pcaconfig_with_flight);
        assertNotNull(config);
        assertEquals(1, config.getAuthorities().size());
        final Authority authority = config.getAuthorities().get(0);
        final AzureActiveDirectoryAuthority azureActiveDirectoryAuthority = (AzureActiveDirectoryAuthority) authority;
        assertFalse(azureActiveDirectoryAuthority.mFlightParameters.isEmpty());
    }

    /**
     * Verify that unknown authority type results in exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUnknownAuthorityException() {
        final PublicClientApplicationConfiguration b2cConfig = loadConfigAndMerge(R.raw.test_pcaconfig_unknown);
        b2cConfig.validateConfiguration();
    }

    /**
     * Verify that unknown audience type results in exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUnknownAudienceException() {
        final PublicClientApplicationConfiguration configWithInvalidAudience = loadConfigAndMerge(R.raw.test_pcaconfig_unknown_audience);
        assertNotNull(configWithInvalidAudience);
        assertFalse(configWithInvalidAudience.getAuthorities().isEmpty());

        final Authority authorityWithInvalidAudience = configWithInvalidAudience.getAuthorities().get(0);
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

    /**
     * Test that the verbose log level can be set via config.
     */
    @Test
    public void testVerboseLogLevel() {
        final PublicClientApplicationConfiguration config = loadConfig(R.raw.test_pcaconfig_log_verbose);
        assertNotNull(config);
        assertNotNull(config.getLoggerConfiguration());
        assertEquals(Logger.LogLevel.VERBOSE, config.getLoggerConfiguration().getLogLevel());
    }

    /**
     * Test that the verbose info level can be set via config.
     */
    @Test
    public void testInfoLogLevel() {
        final PublicClientApplicationConfiguration config = loadConfig(R.raw.test_pcaconfig_log_info);
        assertNotNull(config);
        assertNotNull(config.getLoggerConfiguration());
        assertEquals(Logger.LogLevel.INFO, config.getLoggerConfiguration().getLogLevel());
    }

    /**
     * Test that the warning log level can be set via config.
     */
    @Test
    public void testWarningLogLevel() {
        final PublicClientApplicationConfiguration config = loadConfig(R.raw.test_pcaconfig_log_warning);
        assertNotNull(config);
        assertNotNull(config.getLoggerConfiguration());
        assertEquals(Logger.LogLevel.WARNING, config.getLoggerConfiguration().getLogLevel());
    }

    /**
     * Test that the error log level can be set via config.
     */
    @Test
    public void testErrorLogLevel() {
        final PublicClientApplicationConfiguration config = loadConfig(R.raw.test_pcaconfig_log_error);
        assertNotNull(config);
        assertNotNull(config.getLoggerConfiguration());
        assertEquals(Logger.LogLevel.ERROR, config.getLoggerConfiguration().getLogLevel());
    }

    /**
     * Test that PII logging can be enabled via config.
     */
    @Test
    public void testPiiOn() {
        final PublicClientApplicationConfiguration config = loadConfig(R.raw.test_pcaconfig_log_info);
        assertNotNull(config);
        assertNotNull(config.getLoggerConfiguration());
        assertTrue(config.getLoggerConfiguration().isPiiEnabled());
    }

    /**
     * Test that PII logging can be disabled via config.
     */
    @Test
    public void testPiiOff() {
        final PublicClientApplicationConfiguration config = loadConfig(R.raw.test_pcaconfig_log_error);
        assertNotNull(config);
        assertNotNull(config.getLoggerConfiguration());
        assertFalse(config.getLoggerConfiguration().isPiiEnabled());
    }

    private PublicClientApplicationConfiguration loadConfig(final int resourceId) {
        return PublicClientApplicationConfigurationFactory.loadConfiguration(mContext, resourceId);
    }

    private PublicClientApplicationConfiguration loadConfigAndMerge(final int resourceId) {
        return PublicClientApplicationConfigurationFactory.initializeConfiguration(mContext, resourceId);
    }

    private PublicClientApplicationConfiguration loadConfig(final File file) {
        return PublicClientApplicationConfigurationFactory.loadConfiguration(file);
    }

    @NonNull
    private File getTestConfigFile() {
        return new File(mContext.getFilesDir(), "test.json");
    }

    @NonNull
    private File copyResourceToTestFile(final int resId) throws IOException {
        final InputStream inputStream = mContext.getResources().openRawResource(resId);
        final byte[] buffer = new byte[inputStream.available()];
        assertTrue(inputStream.read(buffer) > 0);
        final String config = new String(buffer);
        final File file = getTestConfigFile();
        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(config);
        }
        return file;
    }
}
