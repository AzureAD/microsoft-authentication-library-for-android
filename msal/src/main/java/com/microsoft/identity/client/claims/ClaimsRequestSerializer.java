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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.List;

class ClaimsRequestSerializer implements JsonSerializer<ClaimsRequest> {

    @Override
    public JsonElement serialize(ClaimsRequest src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject claimsRequest = new JsonObject();
        JsonObject userInfoObject = new JsonObject();
        JsonObject accessTokenObject = new JsonObject();
        JsonObject idTokenObject = new JsonObject();

        addPropertiesToObject(src.getAccessTokenClaimsRequested(), accessTokenObject, context);
        addPropertiesToObject(src.getIdTokenClaimsRequested(), idTokenObject, context);
        addPropertiesToObject(src.getUserInfoClaimsRequested(), userInfoObject, context);

        if (userInfoObject.size() != 0) {
            claimsRequest.add(ClaimsRequest.USERINFO, userInfoObject);
        }

        if (idTokenObject.size() != 0) {
            claimsRequest.add(ClaimsRequest.ID_TOKEN, idTokenObject);
        }

        if (accessTokenObject.size() != 0) {
            claimsRequest.add(ClaimsRequest.ACCESS_TOKEN, accessTokenObject);
        }

        return claimsRequest;
    }

    public void addPropertiesToObject(List<RequestedClaim> requestedClaims,
                                      JsonObject addTo,
                                      JsonSerializationContext context) {
        for (RequestedClaim claim : requestedClaims) {
            addTo.add(claim.getName(), context.serialize(claim.getAdditionalInformation(), RequestedClaimAdditionalInformation.class));
        }
    }

}
