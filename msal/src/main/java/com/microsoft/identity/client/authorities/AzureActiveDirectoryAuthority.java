package com.microsoft.identity.client.authorities;

import android.net.Uri;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class AzureActiveDirectoryAuthority extends Authority {

    @SerializedName("audience")
    public AzureActiveDirectoryAudience mAudience;

    @SerializedName("slice")
    public AzureActiveDirectorySlice mSlice;

    @SerializedName("flight_parameters")
    public Map<String, String> mFlightParameters;

    private AzureActiveDirectoryCloud mAzureActiveDirectoryCloud;

    private void getAzureActiveDirectoryCloud(){

        AzureActiveDirectoryCloud cloud = null;

        try {
            cloud = AzureActiveDirectory.getAzureActiveDirectoryCloud(new URL(mAudience.getCloudUrl()));
            mKnownToMicrosoft = true;
        } catch (MalformedURLException e) {
            cloud = null;
            mKnownToMicrosoft = false;
        }

        mAzureActiveDirectoryCloud = cloud;
    }

    public AzureActiveDirectoryAuthority(AzureActiveDirectoryAudience signInAudience){
        mAudience = signInAudience;
        getAzureActiveDirectoryCloud();
    }

    public AzureActiveDirectoryAuthority(){
        //Defaulting to AllAccounts which maps to the "common" tenant
        mAudience = new AllAccounts();
        getAzureActiveDirectoryCloud();
    }

    public AzureActiveDirectorySlice getSlice(){
        return this.mSlice;
    }

    public Map<String, String> getFlightParameters(){
        return this.mFlightParameters;
    }

    @Override
    public Uri getAuthorityUri() {
        Uri issuer;
        if(mAzureActiveDirectoryCloud == null) {
            issuer = Uri.parse(mAudience.getCloudUrl());
        }else{
            issuer = Uri.parse(mAzureActiveDirectoryCloud.getPreferredNetworkHostName());
        }
        return issuer.buildUpon().appendPath(mAudience.getTenantId()).build();
    }


}
