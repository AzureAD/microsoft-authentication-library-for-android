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

/**
 * MSAL internal class for representing the AAD authority.
 */
class AadAuthority extends Authority {
    private static final String TAG = AadAuthority.class.getSimpleName();
    private static final String AAD_INSTANCE_DISCOVERY_ENDPOINT = "https://login.microsoftonline.com/common/discovery/instance";
    private static final String API_VERSION = "api-version";
    private static final String API_VERSION_VALUE = "1.0";
    private static final String AUTHORIZATION_ENDPOINT = "authorization_endpoint";

    static final String DEPRECATED_AAD_AUTHORITY_HOST = "login.windows.net";
    static final String AAD_AUTHORITY_HOST = "login.microsoftonline.com";
    static final String[] TRUSTED_HOSTS = new String[]{
            AAD_AUTHORITY_HOST, // Microsoft Azure Worldwide
            "login.chinacloudapi.cn", // Microsoft Azure China
            "login.microsoftonline.de", // Microsoft Azure Germany
            "login-us.microsoftonline.com" // Microsoft Azure US government
    };

    static final Set<String> TRUSTED_HOST_SET = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(TRUSTED_HOSTS)));

    /**
     * Constructor for creating the {@link AadAuthority}.
     */
    AadAuthority(final URL authority, boolean validateAuthority) {
        super(authority, validateAuthority);

        if (authority.getHost().equalsIgnoreCase(DEPRECATED_AAD_AUTHORITY_HOST)) {
            try {
                final String hostWithPort = mAuthorityUrl.getAuthority().replace(DEPRECATED_AAD_AUTHORITY_HOST, AAD_AUTHORITY_HOST);
                mAuthorityUrl = new URL(String.format("https://%s%s", hostWithPort, mAuthorityUrl.getPath()));
            } catch (final MalformedURLException e) {
                Logger.errorPII(TAG, null, "Fail to replace login.windows.net to login.microsoftonline.com", e);
                throw new IllegalArgumentException("Malformed authority url");
            }
        }

        mAuthorityType = AuthorityType.AAD;
    }

    @Override
    String performInstanceDiscovery(final RequestContext requestContext, final String userPrincipalName) throws MsalServiceException, MsalClientException {
        Logger.infoPII(TAG, requestContext, "Passed in authority " + mAuthorityUrl.toString() + " is AAD authority. "
                + "Start doing Instance discovery.");
        if (!mValidateAuthority || TRUSTED_HOST_SET.contains(mAuthorityUrl.getAuthority())) {
            Logger.verbose(TAG, requestContext, "Authority validation is turned off or the passed-in authority is "
                    + "in the trust list, skipping instance discovery.");
            return getDefaultOpenIdConfigurationEndpoint();
        }

        final Oauth2Client oauth2Client = new Oauth2Client(requestContext);
        oauth2Client.addQueryParameter(API_VERSION, API_VERSION_VALUE);
        oauth2Client.addQueryParameter(AUTHORIZATION_ENDPOINT, mAuthorityUrl.toString() + DEFAULT_AUTHORIZE_ENDPOINT);
        oauth2Client.addHeader(OauthConstants.OauthHeader.CORRELATION_ID, requestContext.getCorrelationId().toString());

        // send instance discovery request
        final InstanceDiscoveryResponse response;
        try {
            response = oauth2Client.discoveryAADInstance(new URL(AAD_INSTANCE_DISCOVERY_ENDPOINT));
        } catch (final MalformedURLException e) {
            // instance discovery endpoint is hard-coded, if it's ever going wrong, should be found during runtime
            throw new MsalClientException(MsalClientException.MALFORMED_URL, "Malformed URL for instance discovery endpoint.", e);
        } catch (final IOException ioException) {
            throw new MsalClientException(MsalClientException.IO_ERROR, ioException.getMessage(), ioException);
        }

        // TODO: invalid_instance should be returned in this case. But we should get a list of errors that will be returned from server.
        if (!MsalUtils.isEmpty(response.getError())) {
            throw new MsalServiceException(response.getError(), response.getErrorDescription(), response.getHttpStatusCode(), null);
        }

        mIsAuthorityValidated = true;
        Logger.infoPII(TAG, requestContext, "Instance discovery succeeded. Tenant discovery endpoint is: "
                + response.getTenantDiscoveryEndpoint());
        return response.getTenantDiscoveryEndpoint();
    }

    @Override
    boolean existsInResolvedAuthorityCache(final String userPrincipalName) {
        return RESOLVED_AUTHORITY.containsKey(mAuthorityUrl.toString());
    }

    @Override
    void addToResolvedAuthorityCache(final String userPrincipalName) {
        RESOLVED_AUTHORITY.put(mAuthorityUrl.toString(), this);
    }
}
