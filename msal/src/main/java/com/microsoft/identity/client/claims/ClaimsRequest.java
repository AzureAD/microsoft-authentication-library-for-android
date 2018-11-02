package com.microsoft.identity.client.claims;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ClaimsRequest {


    public final static String USERINFO = "userinfo";
    public final static String ID_TOKEN = "id_token";
    public final static String ACCESS_TOKEN = "access_token";

    private List<RequestedClaim> mUserInfoClaimsRequested = new ArrayList<>();
    private List<RequestedClaim> mAccessTokenClaimsRequested = new ArrayList<>();
    private List<RequestedClaim> mIdTokenClaimsRequested = new ArrayList<>();

    public List<RequestedClaim> getUserInfoClaimsRequested(){return mUserInfoClaimsRequested;}
    public List<RequestedClaim> getAccessTokenClaimsRequested(){ return mAccessTokenClaimsRequested;}
    public List<RequestedClaim> getIdTokenClaimsRequested(){ return mIdTokenClaimsRequested;}

    public static ClaimsRequest getClaimsRequestFromJsonString(String claimsRequestJson){
        return deserializeClaimsRequest(claimsRequestJson);
    }

    public static String getJsonStringFromClaimsRequest(ClaimsRequest claimsRequest){
        return serializeClaimsRequest(claimsRequest);
    }

    private static String serializeClaimsRequest(ClaimsRequest claimsRequest){
        GsonBuilder gsonBuilder = new GsonBuilder();

        ClaimsRequestSerializer claimsRequestSerializer = new ClaimsRequestSerializer();
        RequestClaimAdditionalInformationSerializer informationSerializer = new RequestClaimAdditionalInformationSerializer();
        gsonBuilder.registerTypeAdapter(ClaimsRequest.class, claimsRequestSerializer);
        gsonBuilder.registerTypeAdapter(RequestedClaimAdditionalInformation.class, informationSerializer);
        //If you omit this... you won't be requesting an claims that don't have additional info specified
        gsonBuilder.serializeNulls();

        Gson claimsRequestGson = gsonBuilder.create();

        String claimsRequestJson = claimsRequestGson.toJson(claimsRequest);

        return claimsRequestJson;

    }

    private static ClaimsRequest deserializeClaimsRequest(String claimsRequestJson){
        GsonBuilder gsonBuilder = new GsonBuilder();

        ClaimsRequestDeserializer deserializer = new ClaimsRequestDeserializer();
        gsonBuilder.registerTypeAdapter(ClaimsRequest.class, deserializer);

        Gson claimsRequestGson = gsonBuilder.create();

        ClaimsRequest claimsRequest = claimsRequestGson.fromJson(claimsRequestJson, ClaimsRequest.class);

        return claimsRequest;

    }

}
