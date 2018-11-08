package com.microsoft.identity.client;

public class AcquireTokenSilentParameters extends TokenParameters {
    private Boolean mForceRefresh;

    public void setForceRefresh(Boolean forceRefresh){
        mForceRefresh = forceRefresh;
    }

    public Boolean getForceRefresh(){
        return mForceRefresh;
    }


}
