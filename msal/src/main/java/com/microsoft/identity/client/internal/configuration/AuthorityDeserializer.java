package com.microsoft.identity.client.internal.configuration;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.microsoft.identity.client.internal.authorities.ActiveDirectoryFederationServicesAuthority;
import com.microsoft.identity.client.internal.authorities.Authority;
import com.microsoft.identity.client.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.client.internal.authorities.AzureActiveDirectoryB2CAuthority;
import com.microsoft.identity.client.internal.authorities.UnknownAuthority;
import com.microsoft.identity.common.internal.logging.Logger;

import java.lang.reflect.Type;

public class AuthorityDeserializer implements JsonDeserializer<Authority> {

    private static final String TAG = AuthorityDeserializer.class.getSimpleName();

    @Override
    public Authority deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        final String methodName = ":deserialize";
        JsonObject authorityObject = json.getAsJsonObject();
        JsonElement type = authorityObject.get("type");

        if (type != null) {
            switch (type.getAsString()) {
                case "AAD":
                    Logger.verbose(
                            TAG + methodName,
                            "Type: AAD"
                    );
                    return context.deserialize(authorityObject, AzureActiveDirectoryAuthority.class);
                case "B2C":
                    Logger.verbose(
                            TAG + methodName,
                            "Type: B2C"
                    );
                    return context.deserialize(authorityObject, AzureActiveDirectoryB2CAuthority.class);
                case "ADFS":
                    Logger.verbose(
                            TAG + methodName,
                            "Type: ADFS"
                    );
                    return context.deserialize(authorityObject, ActiveDirectoryFederationServicesAuthority.class);
                default:
                    Logger.verbose(
                            TAG + methodName,
                            "Type: Unknown"
                    );
                    return context.deserialize(authorityObject, UnknownAuthority.class);
            }
        }

        return null;
    }
}
