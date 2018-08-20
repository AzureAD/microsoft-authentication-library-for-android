package com.microsoft.identity.client.authorities;

public abstract class AzureActiveDirectoryAudience {

    private String cloudUrl;
    private String tenantId;


    private static final String ORGANIZATIONS = "organizations";
    private static final String CONSUMERS = "consumers";
    private static final String ALL = "common";

    public String getCloudUrl() {
        return cloudUrl;
    }

    public void setCloudUrl(String cloudUrl) {
        this.cloudUrl = cloudUrl;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public static AzureActiveDirectoryAudience getAzureActiveDirectoryAudience(String cloudUrl, String tenantId){

        AzureActiveDirectoryAudience audience = null;

        switch(tenantId.toLowerCase()){
            case ORGANIZATIONS:
                audience = new AnyOrganizationalAccount(cloudUrl);
                break;
            case CONSUMERS:
                audience = new AnyPersonalAccount();
                break;
            case ALL:
                audience = new AllAccounts();
                break;
            default:
                audience = new AccountsInOneOrganization(cloudUrl, tenantId);
        }

        return audience;
    }
}
