package com.microsoft.identity.client.claims;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.List;


public class ClaimsRequestDeserializer implements JsonDeserializer<ClaimsRequest> {
    @Override
    public ClaimsRequest deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ClaimsRequest claimsRequest = new ClaimsRequest();

        addProperties(claimsRequest.getAccessTokenClaimsRequested(), json.getAsJsonObject().getAsJsonObject(ClaimsRequest.ACCESS_TOKEN), context);
        addProperties(claimsRequest.getIdTokenClaimsRequested(), json.getAsJsonObject().getAsJsonObject(ClaimsRequest.ID_TOKEN), context);
        addProperties(claimsRequest.getUserInfoClaimsRequested(), json.getAsJsonObject().getAsJsonObject(ClaimsRequest.USERINFO), context);

        return claimsRequest;
    }

    private void addProperties(List<RequestedClaim> claimList, JsonObject addTo, JsonDeserializationContext context){

        if(addTo == null){
            return;
        }

        for(String key: addTo.keySet()){
            RequestedClaim claim = new RequestedClaim();
            claim.setName(key);
            JsonElement element = addTo.get(key);
            if(!(element instanceof JsonNull)) {
                RequestedClaimAdditionalInformation additionalInformation = context.deserialize(addTo.getAsJsonObject(key), RequestedClaimAdditionalInformation.class);
                claim.setAdditionalInformation(additionalInformation);
            }
            claimList.add(claim);
        }
    }
}
