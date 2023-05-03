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

import com.microsoft.identity.common.java.authorities.AccountsInOneOrganization;
import com.microsoft.identity.common.java.authorities.AnyOrganizationalAccount;
import com.microsoft.identity.common.java.authorities.AnyPersonalAccount;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.java.authorities.Environment;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryEnvironment;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
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
