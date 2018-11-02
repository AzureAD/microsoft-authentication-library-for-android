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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

public class ClaimsRequest {


    public final static String USERINFO = "userinfo";
    public final static String ID_TOKEN = "id_token";
    public final static String ACCESS_TOKEN = "access_token";

    private List<RequestedClaim> mUserInfoClaimsRequested = new ArrayList<>();
    private List<RequestedClaim> mAccessTokenClaimsRequested = new ArrayList<>();
    private List<RequestedClaim> mIdTokenClaimsRequested = new ArrayList<>();

    public List<RequestedClaim> getUserInfoClaimsRequested() {
        return mUserInfoClaimsRequested;
    }

    public List<RequestedClaim> getAccessTokenClaimsRequested() {
        return mAccessTokenClaimsRequested;
    }

    public List<RequestedClaim> getIdTokenClaimsRequested() {
        return mIdTokenClaimsRequested;
    }

    public static ClaimsRequest getClaimsRequestFromJsonString(String claimsRequestJson) {
        return deserializeClaimsRequest(claimsRequestJson);
    }

    public static String getJsonStringFromClaimsRequest(ClaimsRequest claimsRequest) {
        return serializeClaimsRequest(claimsRequest);
    }

    private static String serializeClaimsRequest(ClaimsRequest claimsRequest) {
        GsonBuilder gsonBuilder = new GsonBuilder();

        ClaimsRequestSerializer claimsRequestSerializer = new ClaimsRequestSerializer();
        RequestClaimAdditionalInformationSerializer informationSerializer = new RequestClaimAdditionalInformationSerializer();
        gsonBuilder.registerTypeAdapter(ClaimsRequest.class, claimsRequestSerializer);
        gsonBuilder.registerTypeAdapter(RequestedClaimAdditionalInformation.class, informationSerializer);
        //If you omit this... you won't be requesting an claims that don't have additional info specified
        gsonBuilder.serializeNulls();

        Gson claimsRequestGson = gsonBuilder.create();

        String claimsRequestJson = claimsRequestGson.toJson(claimsRequest);

        return claimsRequestJson;

    }

    private static ClaimsRequest deserializeClaimsRequest(String claimsRequestJson) {
        GsonBuilder gsonBuilder = new GsonBuilder();

        ClaimsRequestDeserializer deserializer = new ClaimsRequestDeserializer();
        gsonBuilder.registerTypeAdapter(ClaimsRequest.class, deserializer);

        Gson claimsRequestGson = gsonBuilder.create();

        ClaimsRequest claimsRequest = claimsRequestGson.fromJson(claimsRequestJson, ClaimsRequest.class);

        return claimsRequest;

    }

    public void requestClaimInAccessToken(String name, RequestedClaimAdditionalInformation additionalInformation){
        requestClaimIn(mAccessTokenClaimsRequested, name, additionalInformation);
    }

    public void requestClaimInIdToken(String name, RequestedClaimAdditionalInformation additionalInformation){
        requestClaimIn(mIdTokenClaimsRequested, name, additionalInformation);
    }

    public void requestClaimInUserInfo(String name, RequestedClaimAdditionalInformation additionalInformation){
        requestClaimIn(mUserInfoClaimsRequested, name, additionalInformation);
    }

    private void requestClaimIn(List<RequestedClaim> claims, String name, RequestedClaimAdditionalInformation additionalInformation){
        RequestedClaim requestedClaim = new RequestedClaim();
        requestedClaim.setName(name);
        requestedClaim.setAdditionalInformation(additionalInformation);
        claims.add(requestedClaim);
    }

}
