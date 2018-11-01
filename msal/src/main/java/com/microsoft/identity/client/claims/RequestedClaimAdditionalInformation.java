package com.microsoft.identity.client.claims;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class RequestedClaimAdditionalInformation {

    @SerializedName("essential")
    private Boolean mEssential = false;

    @SerializedName("values")
    private List<Object> mValues = new ArrayList<>();

    @SerializedName("value")
    private Object mValue = null;

    public void setEssential(Boolean essential){
        mEssential = essential;
    }

    public Boolean getEssential(){
        return mEssential;
    }

    public List<Object> getValues(){
        return mValues;
    }

    public Object getValue(){return mValue;}

    public void setValue(Object value){
        mValue = value;
    }
}
