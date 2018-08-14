package com.microsoft.identity.client.authorities;

public class AccountsInOneOrganization extends AzureActiveDirectoryAudience {

    public AccountsInOneOrganization(String tenantId){
        //TODO: Set default cloud url
        this.setTenantId(tenantId);
    }

    public AccountsInOneOrganization(String cloudUrl, String tenantId){
        this.setCloudUrl(cloudUrl);
        this.setTenantId(tenantId);
    }

}
