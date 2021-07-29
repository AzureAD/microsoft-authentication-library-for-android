package com.microsoft.identity.client;

import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryB2CAuthority;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public final class AuthorityTest {

    public final static String ORGANIZATIONS_AUTHORITY_URL = "https://login.microsoftonline.com/organizations";
    public final static String ALTERNATE_CLOUD_ORGANIZATIONS_AUTHORITY_URL = "https://login.microsoftonline.de/organizations";
    public final static String ORGANIZATIONS_TENANT_AUTHORITY_URL = "https://login.microsoftonline.com/tenantid";
    public final static String ALTERNATE_CLOUD_ORGANIZATIONS_TENANT_AUTHORITY_URL = "https://login.microsoftonline.de/tenantid";
    public final static String COMMON_AUTHORITY_URL = "https://login.microsoftonline.com/common";
    public final static String CONSUMERS_AUTHORITY_URL = "https://login.microsoftonline.com/consumers";


    @Test
    public void testGetAuthorityFromAuthorityUrlAllAccounts() {
        Authority authority = Authority.getAuthorityFromAuthorityUrl(COMMON_AUTHORITY_URL);
        Assert.assertEquals(COMMON_AUTHORITY_URL, authority.getAuthorityURL().toString());
    }

    @Test
    public void testGetAuthorityFromAuthorityUrlAllOrganizations() {
        Authority authority = Authority.getAuthorityFromAuthorityUrl(ORGANIZATIONS_AUTHORITY_URL);
        Assert.assertEquals(ORGANIZATIONS_AUTHORITY_URL, authority.getAuthorityURL().toString());
    }

    @Test
    public void testGetAuthorityFromAuthorityUrlAnyPersonalAccount() {
        Authority authority = Authority.getAuthorityFromAuthorityUrl(CONSUMERS_AUTHORITY_URL);
        Assert.assertEquals(CONSUMERS_AUTHORITY_URL, authority.getAuthorityURL().toString());
    }

    @Test
    public void testGetAuthorityFromAuthorityUrlAlternateCloudAnyOrganization() {
        Authority authority = Authority.getAuthorityFromAuthorityUrl(ALTERNATE_CLOUD_ORGANIZATIONS_AUTHORITY_URL);
        Assert.assertEquals(ALTERNATE_CLOUD_ORGANIZATIONS_AUTHORITY_URL, authority.getAuthorityURL().toString());
    }

    @Test
    public void testGetAuthorityFromAuthorityUrlAlternateCloudOneOrganization() {
        Authority authority = Authority.getAuthorityFromAuthorityUrl(ALTERNATE_CLOUD_ORGANIZATIONS_TENANT_AUTHORITY_URL);
        Assert.assertEquals(ALTERNATE_CLOUD_ORGANIZATIONS_TENANT_AUTHORITY_URL, authority.getAuthorityURL().toString());
    }

    @Test
    public void testGetAuthorityFromAuthorityUrlOneOrganization() {
        Authority authority = Authority.getAuthorityFromAuthorityUrl(ORGANIZATIONS_TENANT_AUTHORITY_URL);
        Assert.assertEquals(ORGANIZATIONS_TENANT_AUTHORITY_URL, authority.getAuthorityURL().toString());
    }

    @Test
    public void testGetAuthorityAAD() {
        // First add a known authority to the Authority class' List
        final String testB2CAuthority = "https://login.microsoftonline.com/common";
        final String type = "AAD";

        final Authority authority = new MockAuthority(testB2CAuthority, type);
        final List<Authority> authorities = new ArrayList<>();
        authorities.add(authority);

        Authority.addKnownAuthorities(authorities);

        final Authority result = Authority.getAuthorityFromAuthorityUrl(testB2CAuthority);
        Assert.assertEquals(AzureActiveDirectoryAuthority.class, result.getClass());
    }

    @Test
    public void testGetAuthorityB2CNonTfp() {
        // First add a known authority to the Authority class' List
        final String testB2CAuthority = "https://contoso.b2clogin.com/B2C_1_SISOPolicy/";
        final String type = "B2C";

        final Authority authority = new MockAuthority(testB2CAuthority, type);
        final List<Authority> authorities = new ArrayList<>();
        authorities.add(authority);

        Authority.addKnownAuthorities(authorities);

        final Authority result = Authority.getAuthorityFromAuthorityUrl(testB2CAuthority);
        Assert.assertEquals(AzureActiveDirectoryB2CAuthority.class, result.getClass());
    }

    private class MockAuthority extends Authority {

        MockAuthority(final String authorityUrl, final String type) {
            super.mAuthorityUrl = authorityUrl;
            super.mAuthorityTypeString = type;
        }

        @Override
        public Uri getAuthorityUri() {
            return Uri.parse(super.mAuthorityUrl);
        }

        @Override
        public URL getAuthorityURL() {
            try {
                return new URL(this.getAuthorityUri().toString());
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Authority URL is not a URL.", e);
            }
        }

        @Override
        public OAuth2Strategy createOAuth2Strategy(final OAuth2StrategyParameters parameters) {
            return null; // Unimplemented...
        }
    }

}
