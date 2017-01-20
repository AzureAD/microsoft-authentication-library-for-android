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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * MSAL internal class for representing the AAD authority.
 */
final class AADAuthority extends Authority {
    private static final String AAD_INSTANCE_DISCOVERY_ENDPOINT = "https://login.windows.net/common/discovery/instance";
    private static final String API_VERSION = "api-version";
    private static final String API_VERSION_VALUE = "1.0";
    private static final String AUTHORIZATION_ENDPOINT = "authorization_endpoint";

    private static final String[] TRUSTED_HOSTS = new String[] {
            "login.windows.net", // Microsoft Azure Worldwide
            "login.microsoftonline.com", // Microsoft Azure Worldwide
            "login.chinacloudapi.cn", // Microsoft Azure China
            "login.microsoftonline.de", // Microsoft Azure Germany
            "login-us.microsoftonline.com" // Microsoft Azure US government
    };

    private static final Set<String> TRUSTED_HOST_SET = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(TRUSTED_HOSTS)));

    /**
     * Constructor for creating the {@link AADAuthority}.
     */
    AADAuthority(final URL authority, boolean validateAuthority) {
        super(authority, validateAuthority);
        mAuthorityType = AuthorityType.AAD;
    }

    @Override
    String performInstanceDiscovery(final UUID correlationId) throws AuthenticationException {
        if (!mValidateAuthority || TRUSTED_HOST_SET.contains(mAuthorityUrl.getAuthority())) {
            return getDefaultOpenIdConfigurationEndpoint();
        }

        final Oauth2Client oauth2Client = new Oauth2Client();
        oauth2Client.addQueryParameter(API_VERSION, API_VERSION_VALUE);
        oauth2Client.addQueryParameter(AUTHORIZATION_ENDPOINT, mAuthorityUrl.toString() + DEFAULT_AUTHORIZE_ENDPOINT);
        oauth2Client.addHeader(OauthConstants.OauthHeader.CORRELATION_ID, correlationId.toString());

        // send instance discovery request
        final InstanceDiscoveryResponse response;
        try {
            response = oauth2Client.discoveryAADInstance(new URL(AAD_INSTANCE_DISCOVERY_ENDPOINT));
        } catch (final MalformedURLException e) {
            // instance discovery endpoint is hard-coded, if it's ever going wrong, should be found during runtime
            throw new AuthenticationException(MSALError.SERVER_ERROR, "Malformed URL for instance discovery endpoint.", e);
        } catch (final RetryableException retryableException) {
            throw new AuthenticationException(MSALError.SERVER_ERROR, retryableException.getMessage(),
                    retryableException.getCause());
        } catch (final IOException ioException) {
            throw new AuthenticationException(MSALError.AUTHORITY_VALIDATION_FAILED, ioException.getMessage(), ioException);
        }

        if (!MSALUtils.isEmpty(response.getError())) {
            throw new AuthenticationException(MSALError.AUTHORITY_VALIDATION_FAILED, "ErrorCode: " + response.getError()
                    + ";ErrorDescription: " + response.getErrorDescription());
        }

        return response.getTenantDiscoveryEndpoint();
    }
}
