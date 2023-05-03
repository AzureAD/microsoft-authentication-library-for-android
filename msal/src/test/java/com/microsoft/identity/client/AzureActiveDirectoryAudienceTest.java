package com.microsoft.identity.client;

import com.microsoft.identity.common.java.authorities.AccountsInOneOrganization;
import com.microsoft.identity.common.java.authorities.AnyOrganizationalAccount;
import com.microsoft.identity.common.java.authorities.AnyPersonalAccount;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAudience;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class AzureActiveDirectoryAudienceTest {

    public final static String CLOUD_URL = "https://login.microsoftonline.com";
    public final static String ALTERNATE_CLOUD_URL = "https://login.microsoftonline.de";
    public final static String ALL_ORGS_TENANT_ID = "organizations";
    public final static String TENANT_ID = "tenantId";
    public final static String CONSUMERS_TENANT_ID = "consumers";
    public final static String ALL_TENANT_ID = "common";

    @Test
    public void testAllOrganizationsAudienceDefaultConstructor() {
        AzureActiveDirectoryAudience audience = new AnyOrganizationalAccount();
        Assert.assertEquals(CLOUD_URL, audience.getCloudUrl());
        Assert.assertEquals(ALL_ORGS_TENANT_ID, audience.getTenantId());
    }

    @Test
    public void testAllOrganizationsAudienceCloudUrlConstructor() {
        AzureActiveDirectoryAudience audience = new AnyOrganizationalAccount(ALTERNATE_CLOUD_URL);
        Assert.assertEquals(ALTERNATE_CLOUD_URL, audience.getCloudUrl());
        Assert.assertEquals(ALL_ORGS_TENANT_ID, audience.getTenantId());
    }

    @Test
    public void testOneOrganizationAudienceDefaultConstructor() {
        AzureActiveDirectoryAudience audience = new AccountsInOneOrganization();
        Assert.assertEquals(CLOUD_URL, audience.getCloudUrl());
    }

    @Test
    public void testOneOrganizationTenantIdConstructor() {
        AzureActiveDirectoryAudience audience = new AccountsInOneOrganization(TENANT_ID);
        Assert.assertEquals(CLOUD_URL, audience.getCloudUrl());
        Assert.assertEquals(TENANT_ID, audience.getTenantId());
    }

    @Test
    public void testOneOrganizationTenantIdAndCloudUrlConstructor() {
        AzureActiveDirectoryAudience audience = new AccountsInOneOrganization(ALTERNATE_CLOUD_URL, TENANT_ID);
        Assert.assertEquals(ALTERNATE_CLOUD_URL, audience.getCloudUrl());
        Assert.assertEquals(TENANT_ID, audience.getTenantId());
    }

    @Test
    public void testConsumersConstructor() {
        AzureActiveDirectoryAudience audience = new AnyPersonalAccount();
        Assert.assertEquals(CLOUD_URL, audience.getCloudUrl());
        Assert.assertEquals(CONSUMERS_TENANT_ID, audience.getTenantId());
    }


}
