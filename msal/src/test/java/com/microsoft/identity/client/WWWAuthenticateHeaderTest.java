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
import com.microsoft.identity.client.claims.WWWAuthenticateHeader;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class WWWAuthenticateHeaderTest {

    public final static String HEADER_SINGLE_QUOTE = "realm='', claims='{\"access_token\":{\"device_id\":null}}', realm=''";
    public final static String HEADER_DOUBLE_QUOTE = "realm=\"\", claims=\"{\"access_token\":{\"device_id\":null}}\", realm=\"\"";
    public final static String HEADER_NOQUOTE = "realm=, claims={\"access_token\":{\"device_id\":null}}, realm=";
    public final static String NO_CLAIMS_DIRECTIVE = "realm=\"\" ";
    public final static String DEVICE_ID_CLAIM_NAME = "device_id";
    public final static String NULL_ADDITIONAL_INFO = null;

    @Test
    public void testHasClaimsDirective() {

        boolean result = WWWAuthenticateHeader.hasClaimsDirective(HEADER_SINGLE_QUOTE);
        Assert.assertEquals(true, result);

    }

    @Test
    public void testDoesNotHaveClaimsDirective() {

        boolean result = WWWAuthenticateHeader.hasClaimsDirective(NO_CLAIMS_DIRECTIVE);
        Assert.assertEquals(false, result);

    }

    @Test
    public void testGetClaimsRequestFromHeaderSingleQuoted() {

        ClaimsRequest claimsRequest = WWWAuthenticateHeader.getClaimsRequestFromWWWAuthenticateHeaderValue(HEADER_SINGLE_QUOTE);

        Assert.assertEquals(DEVICE_ID_CLAIM_NAME, claimsRequest.getAccessTokenClaimsRequested().get(0).getName());
        Assert.assertEquals(NULL_ADDITIONAL_INFO, claimsRequest.getAccessTokenClaimsRequested().get(0).getAdditionalInformation());
    }

    @Test
    public void testGetClaimsRequestFromHeaderDoubleQuoted() {
        ClaimsRequest claimsRequest = WWWAuthenticateHeader.getClaimsRequestFromWWWAuthenticateHeaderValue(HEADER_DOUBLE_QUOTE);

        Assert.assertEquals(DEVICE_ID_CLAIM_NAME, claimsRequest.getAccessTokenClaimsRequested().get(0).getName());
        Assert.assertEquals(NULL_ADDITIONAL_INFO, claimsRequest.getAccessTokenClaimsRequested().get(0).getAdditionalInformation());
    }

    @Test
    public void testGetClaimsRequestFromHeaderNoQuotes() {

        ClaimsRequest claimsRequest = WWWAuthenticateHeader.getClaimsRequestFromWWWAuthenticateHeaderValue(HEADER_NOQUOTE);

        Assert.assertEquals(DEVICE_ID_CLAIM_NAME, claimsRequest.getAccessTokenClaimsRequested().get(0).getName());
        Assert.assertEquals(NULL_ADDITIONAL_INFO, claimsRequest.getAccessTokenClaimsRequested().get(0).getAdditionalInformation());
    }


}
