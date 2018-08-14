package com.microsoft.identity.client.authorities;

public class AnyPersonalAccount extends AzureActiveDirectoryAudience {

    public static final String ANY_PERSONAL_ACCOUNT_TENANT_ID = "consumers";

    public AnyPersonalAccount(){
        this.setTenantId(ANY_PERSONAL_ACCOUNT_TENANT_ID);
    }
}
