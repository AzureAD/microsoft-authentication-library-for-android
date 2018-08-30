package com.microsoft.identity.client.internal.configuration;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.microsoft.identity.client.authorities.ActiveDirectoryFederationServicesAuthority;
import com.microsoft.identity.client.authorities.Authority;
import com.microsoft.identity.client.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.client.authorities.AzureActiveDirectoryB2CAuthority;
import com.microsoft.identity.client.authorities.UnknownAuthority;

import java.lang.reflect.Type;

public class AuthorityDeserializer implements JsonDeserializer<Authority> {

    private static final String TAG = AuthorityDeserializer.class.getSimpleName();

    @Override
    public Authority deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject authorityObject = json.getAsJsonObject();
        JsonElement type = authorityObject.get("type");

        if (type != null) {
            switch (type.getAsString()) {
                case "AAD":
                    return context.deserialize(authorityObject, AzureActiveDirectoryAuthority.class);
                case "B2C":
                    return context.deserialize(authorityObject, AzureActiveDirectoryB2CAuthority.class);
                case "ADFS":
                    return context.deserialize(authorityObject, ActiveDirectoryFederationServicesAuthority.class);
                default:
                    return context.deserialize(authorityObject, UnknownAuthority.class);
            }
        }

        return null;
    }
}
