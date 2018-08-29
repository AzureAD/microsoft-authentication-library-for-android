package com.microsoft.identity.client.internal.configuration;

import android.net.Uri;

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
import com.microsoft.identity.common.internal.logging.Logger;

import java.lang.reflect.Field;
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
                    final AzureActiveDirectoryB2CAuthority b2CAuthority = context.deserialize(authorityObject, AzureActiveDirectoryB2CAuthority.class);

                    try {
                        final JsonObject authorityIn = json.getAsJsonObject();
                        final Field authorityUri = b2CAuthority.getClass().getDeclaredField("mAuthorityUri");
                        authorityUri.setAccessible(true);
                        authorityUri.set(b2CAuthority, Uri.parse(authorityIn.get("authority_url").toString()));
                    } catch (NoSuchFieldException e) {
                        Logger.error(TAG, "Serialization failed to locate field", e);
                    } catch (IllegalAccessException e) {
                        Logger.error(TAG, "Serialization failed to access field", e);
                    }

                    return b2CAuthority;
                case "ADFS":
                    return context.deserialize(authorityObject, ActiveDirectoryFederationServicesAuthority.class);
                default:
                    return context.deserialize(authorityObject, UnknownAuthority.class);
            }
        }

        return null;
    }
}
