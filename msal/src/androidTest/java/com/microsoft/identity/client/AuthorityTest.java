package com.microsoft.identity.client;

import android.support.test.runner.AndroidJUnit4;

import com.microsoft.identity.client.internal.authorities.Authority;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class AuthorityTest {

    public final static String ORGANIZATIONS_AUTHORITY_URL = "https://login.microsoftonline.com/organizations";
    public final static String ALTERNATE_CLOUD_ORGANIZATIONS_AUTHORITY_URL = "https://login.microsoftonline.de/organizations";
    public final static String ORGANIZATIONS_TENANT_AUTHORITY_URL = "https://login.microsoftonline.com/tenantid";
    public final static String ALTERNATE_CLOUD_ORGANIZATIONS_TENANT_AUTHORITY_URL = "https://login.microsoftonline.de/tenantid";
    public final static String COMMON_AUTHORITY_URL = "https://login.microsoftonline.com/common";
    public final static String CONSUMERS_AUTHORITY_URL = "https://login.microsoftonline.com/consumers";


    @Test
    public void testGetAuthorityFromAuthorityUrlAllAccounts(){
        Authority authority = Authority.getAuthorityFromAuthorityUrl(COMMON_AUTHORITY_URL);
        Assert.assertEquals(COMMON_AUTHORITY_URL, authority.getAuthorityURL().toString());
    }

    @Test
    public void testGetAuthorityFromAuthorityUrlAllOrganizations(){
        Authority authority = Authority.getAuthorityFromAuthorityUrl(ORGANIZATIONS_AUTHORITY_URL);
        Assert.assertEquals(ORGANIZATIONS_AUTHORITY_URL, authority.getAuthorityURL().toString());
    }

    @Test
    public void testGetAuthorityFromAuthorityUrlAnyPersonalAccount(){
        Authority authority = Authority.getAuthorityFromAuthorityUrl(CONSUMERS_AUTHORITY_URL);
        Assert.assertEquals(CONSUMERS_AUTHORITY_URL, authority.getAuthorityURL().toString());
    }

    @Test
    public void testGetAuthorityFromAuthorityUrlAlternateCloudAnyOrganization(){
        Authority authority = Authority.getAuthorityFromAuthorityUrl(ALTERNATE_CLOUD_ORGANIZATIONS_AUTHORITY_URL);
        Assert.assertEquals(ALTERNATE_CLOUD_ORGANIZATIONS_AUTHORITY_URL, authority.getAuthorityURL().toString());
    }

    @Test
    public void testGetAuthorityFromAuthorityUrlAlternateCloudOneOrganization(){
        Authority authority = Authority.getAuthorityFromAuthorityUrl(ALTERNATE_CLOUD_ORGANIZATIONS_TENANT_AUTHORITY_URL);
        Assert.assertEquals(ALTERNATE_CLOUD_ORGANIZATIONS_TENANT_AUTHORITY_URL, authority.getAuthorityURL().toString());
    }

    @Test
    public void testGetAuthorityFromAuthorityUrlOneOrganization(){
        Authority authority = Authority.getAuthorityFromAuthorityUrl(ORGANIZATIONS_TENANT_AUTHORITY_URL);
        Assert.assertEquals(ORGANIZATIONS_TENANT_AUTHORITY_URL, authority.getAuthorityURL().toString());
    }

}
