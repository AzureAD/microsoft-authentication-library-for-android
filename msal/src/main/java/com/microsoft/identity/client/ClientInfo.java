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

import android.util.Base64;

import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.internal.MsalUtils;

import org.json.JSONException;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * Msal internal class for representing the client_info returned from token endpoint, client_info is used to uniquely identify
 * an user.
 */

final class ClientInfo {
    private final String mUniqueIdentifier;
    private final String mUniqueTenantIdentifier;

    ClientInfo(final String rawClientInfo) throws MsalClientException {
        if (MsalUtils.isEmpty(rawClientInfo)) {
            mUniqueIdentifier = "";
            mUniqueTenantIdentifier = "";
            return;
        }

        // decode the client info first
        final String decodedClientInfo = new String(Base64.decode(rawClientInfo, Base64.URL_SAFE), Charset.forName(MsalUtils.ENCODING_UTF8));
        final Map<String, String> clientInfoItems;
        try {
            clientInfoItems = MsalUtils.extractJsonObjectIntoMap(decodedClientInfo);
        } catch (final JSONException e) {
            throw new MsalClientException(MsalClientException.JSON_PARSE_FAILURE, "Failed to parse the returned raw client info.");
        }

        mUniqueIdentifier = clientInfoItems.get(ClientInfoClaim.UNIQUE_IDENTIFIER);
        mUniqueTenantIdentifier = clientInfoItems.get(ClientInfoClaim.UNIQUE_TENANT_IDENTIFIER);
    }

    String getUniqueIdentifier() {
        return MsalUtils.isEmpty(mUniqueIdentifier) ? "" : mUniqueIdentifier;
    }

    String getUniqueTenantIdentifier() {
        return MsalUtils.isEmpty(mUniqueTenantIdentifier) ? "" : mUniqueTenantIdentifier;
    }

    static final class ClientInfoClaim {
        static final String UNIQUE_IDENTIFIER = "uid";
        static final String UNIQUE_TENANT_IDENTIFIER = "utid";
    }
}
