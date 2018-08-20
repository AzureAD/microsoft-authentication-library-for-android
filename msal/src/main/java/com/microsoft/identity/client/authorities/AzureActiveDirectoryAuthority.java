package com.microsoft.identity.client.authorities;

import android.net.Uri;

import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;

import java.net.MalformedURLException;
import java.net.URL;

public class AzureActiveDirectoryAuthority extends Authority {

    public AzureActiveDirectoryAudience mAudience;
    private AzureActiveDirectoryCloud mAzureActiveDirectoryCloud;

    private void getAzureActiveDirectoryCloud(){

        AzureActiveDirectoryCloud cloud = null;

        try {
            cloud = AzureActiveDirectory.getAzureActiveDirectoryCloud(new URL(mAudience.getCloudUrl()));
        } catch (MalformedURLException e) {
            cloud = null;
        }

        mAzureActiveDirectoryCloud = cloud;
    }

    public AzureActiveDirectoryAuthority(AzureActiveDirectoryAudience audience){
        mAudience = audience;
        getAzureActiveDirectoryCloud();
    }

    public AzureActiveDirectoryAuthority(){
        //Defaulting to AllAccounts which maps to the "common" tenant
        mAudience = new AllAccounts();
        getAzureActiveDirectoryCloud();
    }

    @Override
    public Uri getAuthorityUri() {
        Uri.Builder builder = new Uri.Builder();
        Uri issuer;
        if(mAzureActiveDirectoryCloud == null) {
            issuer = Uri.parse(mAudience.getCloudUrl());
        }else{
            issuer = Uri.parse(mAzureActiveDirectoryCloud.getPreferredNetworkHostName());
        }
        return issuer.buildUpon().appendPath(mAudience.getTenantId()).build();
    }


}
