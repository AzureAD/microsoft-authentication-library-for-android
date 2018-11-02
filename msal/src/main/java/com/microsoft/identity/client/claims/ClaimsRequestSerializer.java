package com.microsoft.identity.client.claims;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.List;

public class ClaimsRequestSerializer implements JsonSerializer<ClaimsRequest> {


    @Override
    public JsonElement serialize(ClaimsRequest src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject claimsRequest = new JsonObject();
        JsonObject userInfoObject = new JsonObject();
        JsonObject accessTokenObject = new JsonObject();
        JsonObject idTokenObject = new JsonObject();

        addPropertiesToObject(src.getAccessTokenClaimsRequested(), accessTokenObject, context);
        addPropertiesToObject(src.getIdTokenClaimsRequested(), idTokenObject, context);
        addPropertiesToObject(src.getUserInfoClaimsRequested(), userInfoObject, context);

        if(userInfoObject.keySet().size() != 0) {
            claimsRequest.add(ClaimsRequest.USERINFO, userInfoObject);
        }

        if(idTokenObject.keySet().size() != 0) {
            claimsRequest.add(ClaimsRequest.ID_TOKEN, idTokenObject);
        }

        if(accessTokenObject.keySet().size() != 0) {
            claimsRequest.add(ClaimsRequest.ACCESS_TOKEN, accessTokenObject);
        }

        return claimsRequest;
    }

    public void addPropertiesToObject(List<RequestedClaim> requestedClaims, JsonObject addTo, JsonSerializationContext context){
        for(RequestedClaim claim : requestedClaims){
            addTo.add(claim.getName(), context.serialize(claim.getAdditionalInformation(), RequestedClaimAdditionalInformation.class));
        }
    }


}
