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

package com.microsoft.identity.client.internal;

import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.claims.RequestedClaim;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CommandParametersAdapterTest {

    private static final String CLIENT_CAPABILITIES_CLAIM = "xms_cc";

    @Test
    public void testAddClientCapabilitiesWithNullRequestAndNullCapabilitiesReturnsEmptyRequest() {
        final ClaimsRequest result =
                CommandParametersAdapter.addClientCapabilitiesToClaimsRequest(null, null);

        assertNotNull(result);
        assertTrue(result.getAccessTokenClaimsRequested().isEmpty());
    }

    @Test
    public void testAddClientCapabilitiesWithNullRequestAddsCapabilitiesClaim() {
        final ClaimsRequest result =
                CommandParametersAdapter.addClientCapabilitiesToClaimsRequest(null, "cp1,cp2");

        assertNotNull(result);
        final List<RequestedClaim> claims = result.getAccessTokenClaimsRequested();
        assertEquals(1, claims.size());

        final RequestedClaim capabilitiesClaim = claims.get(0);
        assertEquals(CLIENT_CAPABILITIES_CLAIM, capabilitiesClaim.getName());
        assertNotNull(capabilitiesClaim.getAdditionalInformation());

        final List<Object> values = capabilitiesClaim.getAdditionalInformation().getValues();
        assertEquals(2, values.size());
        assertTrue(values.contains("cp1"));
        assertTrue(values.contains("cp2"));
    }

    @Test
    public void testAddClientCapabilitiesPreservesExistingRequestInstance() {
        final ClaimsRequest existing = new ClaimsRequest();

        final ClaimsRequest result =
                CommandParametersAdapter.addClientCapabilitiesToClaimsRequest(existing, null);

        assertSame(existing, result);
        assertTrue(result.getAccessTokenClaimsRequested().isEmpty());
    }

    @Test
    public void testAddClientCapabilitiesSplitsSingleCapability() {
        final ClaimsRequest result =
                CommandParametersAdapter.addClientCapabilitiesToClaimsRequest(null, "cp1");

        final List<RequestedClaim> claims = result.getAccessTokenClaimsRequested();
        assertEquals(1, claims.size());
        assertEquals(CLIENT_CAPABILITIES_CLAIM, claims.get(0).getName());

        final List<Object> values = claims.get(0).getAdditionalInformation().getValues();
        assertEquals(1, values.size());
        assertTrue(values.contains("cp1"));
    }
}
