package com.microsoft.identity.client.authorities;

public class AnyOrganizationalAccount extends AzureActiveDirectoryAudience {

    public AnyOrganizationalAccount(){
        this.setCloudUrl(""); // TODO: Default
        this.setTenantId("organizations");

    }
    public AnyOrganizationalAccount(String cloudUrl){
        this.setCloudUrl(cloudUrl);
    }
}
