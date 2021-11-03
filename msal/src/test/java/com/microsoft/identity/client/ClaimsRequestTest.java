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

import junit.framework.Assert;

import org.junit.Test;

public class ClaimsRequestTest {

    public static final String DEVICE_ID_CLAIM_NAME = "device_id";
    public static final String POLICY_ID_CLAIM_NAME = "policy_id";
    public static final String[] POLICY_ID_VALUES = {"policy1", "policy2"};

    @Test
    public void testSerializeBasicClaimsRequest() {

        // What we're aiming for
        final String CLAIMS_REQUEST_JSON = "{\"access_token\":{\"device_id\":null}}";

        ClaimsRequest cr = new ClaimsRequest();
        cr.requestClaimInAccessToken(DEVICE_ID_CLAIM_NAME, null);

        String claimsRequestJson = ClaimsRequest.getJsonStringFromClaimsRequest(cr);

        Assert.assertEquals(CLAIMS_REQUEST_JSON, claimsRequestJson);
    }

    @Test
    public void testSerializeBasicClaimsWithInfoRequest() {

        // What we're aiming for
        final String CLAIMS_REQUEST_JSON =
                "{\"access_token\":{\"device_id\":{\"essential\":true}}}";

        ClaimsRequest cr = new ClaimsRequest();

        RequestedClaimAdditionalInformation additionalInformation =
                new RequestedClaimAdditionalInformation();
        additionalInformation.setEssential(true);
        cr.requestClaimInAccessToken(DEVICE_ID_CLAIM_NAME, additionalInformation);

        String claimsRequestJson = ClaimsRequest.getJsonStringFromClaimsRequest(cr);

        Assert.assertEquals(CLAIMS_REQUEST_JSON, claimsRequestJson);
    }

    @Test
    public void testSerializeBasicClaimsWithInfoAndValuesRequest() {

        // What we're aiming for
        final String CLAIMS_REQUEST_JSON =
                "{\"access_token\":{\"policy_id\":{\"essential\":true,\"values\":[\"policy1\",\"policy2\"]}}}";

        ClaimsRequest cr = new ClaimsRequest();

        RequestedClaimAdditionalInformation additionalInformation =
                new RequestedClaimAdditionalInformation();
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
}
