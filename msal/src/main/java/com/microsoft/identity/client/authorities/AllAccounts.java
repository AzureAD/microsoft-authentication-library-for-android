package com.microsoft.identity.client.authorities;

public class AllAccounts extends AzureActiveDirectoryAudience {

    public static final String ALL_ACCOUNTS_TENANT_ID = "common";

    public AllAccounts (){
        this.setCloudUrl("");  //TODO: Default
        this.setTenantId(ALL_ACCOUNTS_TENANT_ID);
    }
}
