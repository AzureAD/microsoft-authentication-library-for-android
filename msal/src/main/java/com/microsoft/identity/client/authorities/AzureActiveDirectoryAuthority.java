package com.microsoft.identity.client.authorities;

import android.net.Uri;

public class AzureActiveDirectoryAuthority extends Authority {

    public AzureActiveDirectoryAudience audience;

    @Override
    public Uri getIssuerUri() {
        Uri.Builder builder = new Uri.Builder();
        Uri issuer = Uri.parse(audience.getCloudUrl());
        return issuer.buildUpon().appendPath(audience.getTenantId()).build();
    }
}
