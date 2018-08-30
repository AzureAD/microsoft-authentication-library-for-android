//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
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
