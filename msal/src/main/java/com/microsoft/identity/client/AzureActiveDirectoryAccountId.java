package com.microsoft.identity.client;

public class AzureActiveDirectoryAccountId extends AccountId {

    private String mObjectId;
    private String mTenantId;

    void setObjectId(final String objectId) {
        mObjectId = objectId;
    }

    public String getObjectId() {
        return mObjectId;
    }

    void setTenantId(final String tenantId) {
        mTenantId = tenantId;
    }

    public String getTenantId() {
        return mTenantId;
    }

}
