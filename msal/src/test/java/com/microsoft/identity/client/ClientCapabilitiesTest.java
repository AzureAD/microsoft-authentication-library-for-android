// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client;


import androidx.annotation.NonNull;

import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation;
import com.microsoft.identity.client.internal.controllers.OperationParametersAdapter;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

public final class ClientCapabilitiesTest {

    public final static String CP1_CAPABILITY = "CP1";


    @Test
    public void testAddClientCapabilitiesMatching() {
        ClaimsRequest clientCapabilities = OperationParametersAdapter.addClientCapabilitiesToClaimsRequest(null, "CP1");
        Assert.assertEquals(clientCapabilities, getAccessTokenClaimsRequest(OperationParametersAdapter.CLIENT_CAPABILITIES_CLAIM, CP1_CAPABILITY));
    }

    @Test
    public void testAddClientCapabilitiesNotMatching() {
        ClaimsRequest clientCapabilities = OperationParametersAdapter.addClientCapabilitiesToClaimsRequest(null, "CP2");
        Assert.assertNotEquals(clientCapabilities, getAccessTokenClaimsRequest(OperationParametersAdapter.CLIENT_CAPABILITIES_CLAIM, CP1_CAPABILITY));
    }

    private ClaimsRequest getAccessTokenClaimsRequest(@NonNull String claimName, @NonNull String claimValue) {
        ClaimsRequest cp1ClaimsRequest = new ClaimsRequest();
        RequestedClaimAdditionalInformation info = new RequestedClaimAdditionalInformation();
        info.setValues(new ArrayList<Object>(Arrays.asList(claimValue)));
        cp1ClaimsRequest.requestClaimInAccessToken(claimName, info);
        return cp1ClaimsRequest;
    }

}
