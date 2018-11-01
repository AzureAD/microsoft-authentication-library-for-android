package com.microsoft.identity.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.claims.ClaimsRequestSerializer;
import com.microsoft.identity.client.claims.RequestClaimAdditionalInformationSerializer;
import com.microsoft.identity.client.claims.RequestedClaim;
import com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation;

import junit.framework.Assert;

import org.junit.Test;

public class ClaimsRequestTest {

    public final static String DEVICE_ID_CLAIM_NAME = "device_id";
    public final static String POLICY_ID_CLAIM_NAME = "policy_id";
    public final static String[] POLICY_ID_VALUES = {"policy1", "policy2"};



    private String getSerializedClaimsRequest(ClaimsRequest claimsRequest){
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

    @Test
    public void testSerializeBasicClaimsRequest(){

        //What we're aiming for
        final String CLAIMS_REQUEST_JSON = "{\"access_token\":{\"device_id\":null}}";

        ClaimsRequest cr = new ClaimsRequest();
        RequestedClaim requestedClaim = new RequestedClaim();
        requestedClaim.setName(DEVICE_ID_CLAIM_NAME);
        cr.getAccessTokenClaimsRequested().add(requestedClaim);

        String claimsRequestJson = getSerializedClaimsRequest(cr);

        Assert.assertEquals(CLAIMS_REQUEST_JSON, claimsRequestJson);

    }

    @Test
    public void testSerializeBasicClaimsWithInfoRequest(){

        //What we're aiming for
        final String CLAIMS_REQUEST_JSON = "{\"access_token\":{\"device_id\":{\"essential\":true}}}";

        ClaimsRequest cr = new ClaimsRequest();
        RequestedClaim requestedClaim = new RequestedClaim();

        RequestedClaimAdditionalInformation additionalInformation = new RequestedClaimAdditionalInformation();
        additionalInformation.setEssential(true);
        requestedClaim.setName(DEVICE_ID_CLAIM_NAME);
        requestedClaim.setAdditionalInformation(additionalInformation);
        cr.getAccessTokenClaimsRequested().add(requestedClaim);

        String claimsRequestJson = getSerializedClaimsRequest(cr);

        Assert.assertEquals(CLAIMS_REQUEST_JSON, claimsRequestJson);

    }

    @Test
    public void testSerializeBasicClaimsWithInfoAndValuesRequest(){

        //What we're aiming for
        final String CLAIMS_REQUEST_JSON = "{\"access_token\":{\"policy_id\":{\"essential\":true,\"values\":[\"policy1\",\"policy2\"]}}}";

        ClaimsRequest cr = new ClaimsRequest();
        RequestedClaim requestedClaim = new RequestedClaim();

        RequestedClaimAdditionalInformation additionalInformation = new RequestedClaimAdditionalInformation();
        additionalInformation.setEssential(true);

        for(String value: POLICY_ID_VALUES){
            additionalInformation.getValues().add(value);
        }

        requestedClaim.setName(POLICY_ID_CLAIM_NAME);


        requestedClaim.setAdditionalInformation(additionalInformation);
        cr.getAccessTokenClaimsRequested().add(requestedClaim);

        String claimsRequestJson = getSerializedClaimsRequest(cr);

        Assert.assertEquals(CLAIMS_REQUEST_JSON, claimsRequestJson);

    }





}
