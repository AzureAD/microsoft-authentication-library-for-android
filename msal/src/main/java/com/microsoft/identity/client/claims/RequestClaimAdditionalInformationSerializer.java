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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

class RequestClaimAdditionalInformationSerializer
        implements JsonSerializer<RequestedClaimAdditionalInformation> {

    @Override
    public JsonElement serialize(
            RequestedClaimAdditionalInformation src,
            Type typeOfSrc,
            JsonSerializationContext context) {

        JsonObject info = new JsonObject();

        info.addProperty(
                RequestedClaimAdditionalInformation.SerializedNames.ESSENTIAL, src.getEssential());

        if (src.getValue() != null) {
            info.addProperty(
                    RequestedClaimAdditionalInformation.SerializedNames.VALUE,
                    src.getValue().toString());
        }

        if (src.getValues().size() > 0) {
            JsonArray valuesArray = new JsonArray();
            for (Object value : src.getValues()) {
                valuesArray.add(value.toString());
            }
            info.add(RequestedClaimAdditionalInformation.SerializedNames.VALUES, valuesArray);
        }

        return info;
    }
}
