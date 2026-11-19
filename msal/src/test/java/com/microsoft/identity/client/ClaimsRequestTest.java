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
package com.microsoft.identity.client;

import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

public class ClaimsRequestTest {

    public final static String DEVICE_ID_CLAIM_NAME = "device_id";
    public final static String POLICY_ID_CLAIM_NAME = "policy_id";
    public final static String[] POLICY_ID_VALUES = {"policy1", "policy2"};


    @Test
    public void testSerializeBasicClaimsRequest() {

        //What we're aiming for
        final String CLAIMS_REQUEST_JSON = "{\"access_token\":{\"device_id\":null}}";

        ClaimsRequest cr = new ClaimsRequest();
        cr.requestClaimInAccessToken(DEVICE_ID_CLAIM_NAME, null);

        String claimsRequestJson = ClaimsRequest.getJsonStringFromClaimsRequest(cr);

        Assert.assertEquals(CLAIMS_REQUEST_JSON, claimsRequestJson);

    }

    @Test
    public void testSerializeBasicClaimsWithInfoRequest() {

        //What we're aiming for
        final String CLAIMS_REQUEST_JSON = "{\"access_token\":{\"device_id\":{\"essential\":true}}}";

        ClaimsRequest cr = new ClaimsRequest();

        RequestedClaimAdditionalInformation additionalInformation = new RequestedClaimAdditionalInformation();
        additionalInformation.setEssential(true);
        cr.requestClaimInAccessToken(DEVICE_ID_CLAIM_NAME, additionalInformation);

        String claimsRequestJson = ClaimsRequest.getJsonStringFromClaimsRequest(cr);

        Assert.assertEquals(CLAIMS_REQUEST_JSON, claimsRequestJson);

    }

    @Test
    public void testSerializeBasicClaimsWithInfoAndValuesRequest() {

        //What we're aiming for
        final String CLAIMS_REQUEST_JSON = "{\"access_token\":{\"policy_id\":{\"essential\":true,\"values\":[\"policy1\",\"policy2\"]}}}";

        ClaimsRequest cr = new ClaimsRequest();

        RequestedClaimAdditionalInformation additionalInformation = new RequestedClaimAdditionalInformation();
        additionalInformation.setEssential(true);

        for (String value : POLICY_ID_VALUES) {
            additionalInformation.getValues().add(value);
        }

        cr.requestClaimInAccessToken(POLICY_ID_CLAIM_NAME, additionalInformation);

        String claimsRequestJson = ClaimsRequest.getJsonStringFromClaimsRequest(cr);

        Assert.assertEquals(CLAIMS_REQUEST_JSON, claimsRequestJson);

    }

    @Test
    public void testSerializeNullClaimsRequest() {

        final String claimsRequestJson = ClaimsRequest.getJsonStringFromClaimsRequest(null);
        Assert.assertEquals(null, claimsRequestJson);

    }

    @Test
    public void testAdditionalPropertiesRoundTripForIdAndAccessTokens() {
        for (final String token : Arrays.asList("id_token", "access_token")) {
            final String json = "{\"" + token + "\":{\"custom_claim\":{\"essential\":true,"
                    + "\"value\":\"one\",\"values\":[\"two\",\"three\"],"
                    + "\"additionalProperties\":[\"property-one\",\"property-two\"]}}}";
            final ClaimsRequest parsed = ClaimsRequest.getClaimsRequestFromJsonString(json);
            Assert.assertEquals(JsonParser.parseString(json),
                    JsonParser.parseString(ClaimsRequest.getJsonStringFromClaimsRequest(parsed)));
        }
    }

    @Test
    public void testTypedAdditionalPropertiesAndDefensiveCopy() {
        final RequestedClaimAdditionalInformation info = new RequestedClaimAdditionalInformation();
        final List<String> properties = new ArrayList<>(Arrays.asList("property-one", "property-two"));
        info.setAdditionalProperties(properties);
        properties.clear();
        final ClaimsRequest request = new ClaimsRequest();
        request.requestClaimInIdToken("custom_claim", info);
        final ClaimsRequest reconstructed = ClaimsRequest.getClaimsRequestFromJsonString(
                ClaimsRequest.getJsonStringFromClaimsRequest(request));
        Assert.assertEquals(request, reconstructed);
        Assert.assertEquals(request.hashCode(), reconstructed.hashCode());
        Assert.assertEquals(Arrays.asList("property-one", "property-two"),
                reconstructed.getIdTokenClaimsRequested().get(0)
                        .getAdditionalInformation().getAdditionalProperties());
        info.setAdditionalProperties(Collections.singletonList("property-other"));
        Assert.assertFalse(request.equals(reconstructed));
    }

    @Test
    public void testEmptyAndAbsentAdditionalPropertiesRemainDistinct() {
        final RequestedClaimAdditionalInformation info = new RequestedClaimAdditionalInformation();
        final ClaimsRequest request = new ClaimsRequest();
        request.requestClaimInIdToken("custom_claim", info);
        Assert.assertEquals("{\"id_token\":{\"custom_claim\":{\"essential\":false}}}",
                ClaimsRequest.getJsonStringFromClaimsRequest(request));
        info.setAdditionalProperties(Collections.<String>emptyList());
        Assert.assertEquals("{\"id_token\":{\"custom_claim\":{\"essential\":false,\"additionalProperties\":[]}}}",
                ClaimsRequest.getJsonStringFromClaimsRequest(request));
    }


}
