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

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the claims request parameter as an object
 *
 * @see <a href="https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter">https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter</a>
 */
public class ClaimsRequest {


    public final static String USERINFO = "userinfo";
    public final static String ID_TOKEN = "id_token";
    public final static String ACCESS_TOKEN = "access_token";

    private List<RequestedClaim> mUserInfoClaimsRequested = new ArrayList<>();
    private List<RequestedClaim> mAccessTokenClaimsRequested = new ArrayList<>();
    private List<RequestedClaim> mIdTokenClaimsRequested = new ArrayList<>();

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClaimsRequest)) return false;

        ClaimsRequest that = (ClaimsRequest) o;

        if (mUserInfoClaimsRequested != null ? !mUserInfoClaimsRequested.equals(that.mUserInfoClaimsRequested) : that.mUserInfoClaimsRequested != null)
            return false;
        if (mAccessTokenClaimsRequested != null ? !mAccessTokenClaimsRequested.equals(that.mAccessTokenClaimsRequested) : that.mAccessTokenClaimsRequested != null)
            return false;
        return mIdTokenClaimsRequested != null ? mIdTokenClaimsRequested.equals(that.mIdTokenClaimsRequested) : that.mIdTokenClaimsRequested == null;
    }
    //CHECKSTYLE:ON

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public int hashCode() {
        int result = mUserInfoClaimsRequested != null ? mUserInfoClaimsRequested.hashCode() : 0;
        result = 31 * result + (mAccessTokenClaimsRequested != null ? mAccessTokenClaimsRequested.hashCode() : 0);
        result = 31 * result + (mIdTokenClaimsRequested != null ? mIdTokenClaimsRequested.hashCode() : 0);
        return result;
    }
    //CHECKSTYLE:ON

    /**
     * Return the list of requested claims for the userinfo endpoint in the claims request parameter object
     *
     * @return
     */
    public List<RequestedClaim> getUserInfoClaimsRequested() {
        return mUserInfoClaimsRequested;
    }

    /**
     * Return the list of requested claims for an Access Token in the claims request parameter object
     *
     * @return
     */
    public List<RequestedClaim> getAccessTokenClaimsRequested() {
        return mAccessTokenClaimsRequested;
    }

    /**
     * Return the list of requested claims for an ID Token in the claims request parameter object
     *
     * @return
     */
    public List<RequestedClaim> getIdTokenClaimsRequested() {
        return mIdTokenClaimsRequested;
    }

    /**
     * Returns a claims request parameter object based on the JSON representation of the same.
     *
     * @param claimsRequestJson
     * @return
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter">https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter</a>
     */
    public static ClaimsRequest getClaimsRequestFromJsonString(String claimsRequestJson) {
        return deserializeClaimsRequest(claimsRequestJson);
    }

    /**
     * Returns the JSON representation of the claims request parameter
     *
     * @param claimsRequest
     * @return
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter">https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter</a>
     */
    public static String getJsonStringFromClaimsRequest(@Nullable final ClaimsRequest claimsRequest) {
        return serializeClaimsRequest(claimsRequest);
    }

    private static String serializeClaimsRequest(@Nullable final ClaimsRequest claimsRequest) {
        final GsonBuilder gsonBuilder = new GsonBuilder();

        final ClaimsRequestSerializer claimsRequestSerializer = new ClaimsRequestSerializer();
        final RequestClaimAdditionalInformationSerializer informationSerializer =
                new RequestClaimAdditionalInformationSerializer();

        gsonBuilder.registerTypeAdapter(ClaimsRequest.class, claimsRequestSerializer);
        gsonBuilder.registerTypeAdapter(RequestedClaimAdditionalInformation.class, informationSerializer);
        //If you omit this... you won't be requesting an claims that don't have additional info specified
        gsonBuilder.serializeNulls();

        final Gson claimsRequestGson = gsonBuilder.create();

        final String claimsRequestJson = claimsRequest != null ? claimsRequestGson.toJson(claimsRequest) : null;

        return claimsRequestJson;

    }

    private static ClaimsRequest deserializeClaimsRequest(@Nullable final String claimsRequestJson) {
        final GsonBuilder gsonBuilder = new GsonBuilder();

        final ClaimsRequestDeserializer deserializer = new ClaimsRequestDeserializer();
        gsonBuilder.registerTypeAdapter(ClaimsRequest.class, deserializer);

        final Gson claimsRequestGson = gsonBuilder.create();

        final ClaimsRequest claimsRequest = claimsRequestGson.fromJson(claimsRequestJson, ClaimsRequest.class);

        return claimsRequest;

    }

    /**
     * Adds a request for a specific claim to be included in an access token via the claims request parameter
     *
     * @param name
     * @param additionalInformation
     */
    public void requestClaimInAccessToken(String name, RequestedClaimAdditionalInformation additionalInformation) {
        requestClaimIn(mAccessTokenClaimsRequested, name, additionalInformation);
    }

    /**
     * Adds a request for a specific claim to be included in an id token via the claims request parameter
     *
     * @param name
     * @param additionalInformation
     */
    public void requestClaimInIdToken(String name, RequestedClaimAdditionalInformation additionalInformation) {
        requestClaimIn(mIdTokenClaimsRequested, name, additionalInformation);
    }

    /**
     * Adds a request for a specific claim to be included in the userinfo response via the claims request parameter
     *
     * @param name
     * @param additionalInformation
     */
    public void requestClaimInUserInfo(String name, RequestedClaimAdditionalInformation additionalInformation) {
        requestClaimIn(mUserInfoClaimsRequested, name, additionalInformation);
    }

    private void requestClaimIn(List<RequestedClaim> claims, String name, RequestedClaimAdditionalInformation additionalInformation) {
        RequestedClaim requestedClaim = new RequestedClaim();
        requestedClaim.setName(name);
        requestedClaim.setAdditionalInformation(additionalInformation);
        claims.add(requestedClaim);
    }

}
