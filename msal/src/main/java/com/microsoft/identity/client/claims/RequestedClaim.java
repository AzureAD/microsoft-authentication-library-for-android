package com.microsoft.identity.client.claims;

public class RequestedClaim {

    private String mName;
    private RequestedClaimAdditionalInformation mInformation;

    public String getName(){
        return mName;
    }

    public void setName(String name){
        mName = name;
    }

    public RequestedClaimAdditionalInformation getAdditionalInformation(){
        return mInformation;
    }

    public void setAdditionalInformation(RequestedClaimAdditionalInformation information){
        mInformation = information;
    }



}
