package com.microsoft.identity.client;

import com.microsoft.identity.common.internal.authorities.AccountsInOneOrganization;
import com.microsoft.identity.common.internal.authorities.AnyOrganizationalAccount;
import com.microsoft.identity.common.internal.authorities.AnyPersonalAccount;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.internal.authorities.Environment;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryEnvironment;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AzureActiveDirectoryAudiencePreProductionTest {

    public final static String ALTERNATE_CLOUD_URL = "https://login.microsoftonline.de";
    public final static String ALL_ORGS_TENANT_ID = "organizations";
    public final static String TENANT_ID = "tenantId";
    public final static String CONSUMERS_TENANT_ID = "consumers";


    @Before
    public void init(){
        AzureActiveDirectory.setEnvironment(Environment.PreProduction);
    }

    @After
    public void finalize(){
        AzureActiveDirectory.setEnvironment(Environment.Production);
    }

    @Test
    public void testAllOrganizationsAudienceDefaultConstructor() {
        AzureActiveDirectoryAudience audience = new AnyOrganizationalAccount();
        Assert.assertEquals(AzureActiveDirectoryEnvironment.PREPRODUCTION_CLOUD_URL, audience.getCloudUrl());
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
        Assert.assertEquals(AzureActiveDirectoryEnvironment.PREPRODUCTION_CLOUD_URL, audience.getCloudUrl());
    }

    @Test
    public void testOneOrganizationTenantIdConstructor() {
        AzureActiveDirectoryAudience audience = new AccountsInOneOrganization(TENANT_ID);
        Assert.assertEquals(AzureActiveDirectoryEnvironment.PREPRODUCTION_CLOUD_URL, audience.getCloudUrl());
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
        Assert.assertEquals(AzureActiveDirectoryEnvironment.PREPRODUCTION_CLOUD_URL, audience.getCloudUrl());
        Assert.assertEquals(CONSUMERS_TENANT_ID, audience.getTenantId());
    }


}
