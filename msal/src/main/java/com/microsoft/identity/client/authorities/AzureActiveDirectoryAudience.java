package com.microsoft.identity.client.authorities;

public abstract class AzureActiveDirectoryAudience {

    private String cloudUrl;
    private String tenantId;


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
}
