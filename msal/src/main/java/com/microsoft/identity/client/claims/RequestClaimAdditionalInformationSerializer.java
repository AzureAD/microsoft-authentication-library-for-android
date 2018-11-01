package com.microsoft.identity.client.claims;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class RequestClaimAdditionalInformationSerializer implements JsonSerializer<RequestedClaimAdditionalInformation> {

    @Override
    public JsonElement serialize(RequestedClaimAdditionalInformation src, Type typeOfSrc, JsonSerializationContext context) {

        JsonObject info = new JsonObject();

        info.addProperty("essential", src.getEssential());

        if(src.getValue() != null){
            info.addProperty("value", src.getValue().toString());
        }

        if(src.getValues().size() > 0){
            JsonArray valuesArray = new JsonArray();
            for(Object value : src.getValues()){
                valuesArray.add(value.toString());
            }
            info.add("values", valuesArray);
        }

        return info;
    }
}
