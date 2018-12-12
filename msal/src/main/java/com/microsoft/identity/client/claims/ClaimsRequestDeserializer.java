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
package com.microsoft.identity.client.claims;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.List;


class ClaimsRequestDeserializer implements JsonDeserializer<ClaimsRequest> {
    @Override
    public ClaimsRequest deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ClaimsRequest claimsRequest = new ClaimsRequest();

        addProperties(claimsRequest.getAccessTokenClaimsRequested(), json.getAsJsonObject().getAsJsonObject(ClaimsRequest.ACCESS_TOKEN), context);
        addProperties(claimsRequest.getIdTokenClaimsRequested(), json.getAsJsonObject().getAsJsonObject(ClaimsRequest.ID_TOKEN), context);
        addProperties(claimsRequest.getUserInfoClaimsRequested(), json.getAsJsonObject().getAsJsonObject(ClaimsRequest.USERINFO), context);

        return claimsRequest;
    }

    private void addProperties(List<RequestedClaim> claimList, JsonObject addTo, JsonDeserializationContext context) {

        if (addTo == null) {
            return;
        }

        for (String key : addTo.keySet()) {
            RequestedClaim claim = new RequestedClaim();
            claim.setName(key);
            JsonElement element = addTo.get(key);
            if (!(element instanceof JsonNull)) {
                RequestedClaimAdditionalInformation additionalInformation = context.deserialize(
                        addTo.getAsJsonObject(key),
                        RequestedClaimAdditionalInformation.class
                );
                claim.setAdditionalInformation(additionalInformation);
            }
            claimList.add(claim);
        }
    }
}
