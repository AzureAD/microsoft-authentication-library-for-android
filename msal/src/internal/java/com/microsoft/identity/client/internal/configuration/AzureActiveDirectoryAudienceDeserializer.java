package com.microsoft.identity.client.internal.configuration;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.microsoft.identity.client.authorities.AccountsInOneOrganization;
import com.microsoft.identity.client.authorities.AllAccounts;
import com.microsoft.identity.client.authorities.AnyOrganizationalAccount;
import com.microsoft.identity.client.authorities.AnyPersonalAccount;
import com.microsoft.identity.client.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.client.authorities.UnknownAudience;

import java.lang.reflect.Type;

public class AzureActiveDirectoryAudienceDeserializer implements JsonDeserializer<AzureActiveDirectoryAudience> {

    @Override
    public AzureActiveDirectoryAudience deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject audienceObject = json.getAsJsonObject();
        JsonElement type = audienceObject.get("type");

        if (type != null) {
            switch (type.getAsString()) {
                case "AzureADMyOrg":
                    return context.deserialize(audienceObject, AccountsInOneOrganization.class);
                case "AzureADMultipleOrgs":
                    return context.deserialize(audienceObject, AnyOrganizationalAccount.class);
                case "AzureADandPersonalMicrosoftAccount":
                    return context.deserialize(audienceObject, AllAccounts.class);
                case "PersonalMicrosoftAccount":
                    return context.deserialize(audienceObject, AnyPersonalAccount.class);
                default:
                    return context.deserialize(audienceObject, UnknownAudience.class);
            }
        }

        return null;
    }
}
