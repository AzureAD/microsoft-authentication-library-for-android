package com.microsoft.identity.client.authorities;

import com.google.gson.annotations.SerializedName;

public class AzureActiveDirectorySlice {

    @SerializedName("slice")
    private String mSlice;
    @SerializedName("dc")
    private String mDataCenter;

    public String getSlice(){
        return mSlice;
    }

    public String getDC(){
        return mDataCenter;
    }

}
