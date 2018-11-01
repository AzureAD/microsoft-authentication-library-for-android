package com.microsoft.identity.client.claims;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ClaimsRequest {


    @SerializedName("userinfo")
    private List<RequestedClaim> mUserInfoClaimsRequested = new ArrayList<>();
    @SerializedName("id_token")
    private List<RequestedClaim> mAccessTokenClaimsRequested = new ArrayList<>();
    @SerializedName("access_token")
    private List<RequestedClaim> mIdTokenClaimsRequested = new ArrayList<>();

    public List<RequestedClaim> getUserInfoClaimsRequested(){return mUserInfoClaimsRequested;}
    public List<RequestedClaim> getAccessTokenClaimsRequested(){ return mAccessTokenClaimsRequested;}
    public List<RequestedClaim> getIdTokenClaimsRequested(){ return mIdTokenClaimsRequested;}

    public static ClaimsRequest getClaimsRequestFromJsonString(String claimsRequest){
        throw new UnsupportedOperationException();
    }



}
