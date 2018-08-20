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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * MSAL internal representation for the authority.
 */
abstract class AuthorityMetadata {
    private static final String TAG = AuthorityMetadata.class.getSimpleName();
    private static final String HTTPS_PROTOCOL = "https";

    static final ConcurrentMap<String, AuthorityMetadata> RESOLVED_AUTHORITY = new ConcurrentHashMap<>();
    static final String DEFAULT_OPENID_CONFIGURATION_ENDPOINT = "/v2.0/.well-known/openid-configuration";
    // default_authorize_endpoint is used for instance discovery sent as query parameter for instance discovery.
    static final String DEFAULT_AUTHORIZE_ENDPOINT = "/oauth2/v2.0/authorize";
    static final String[] TENANTLESS_TENANT_NAME = {"common", "organizations"};

    static final String ADFS_AUTHORITY_PREFIX = "adfs";
    static final String B2C_AUTHORITY_PREFIX = "tfp";

    final boolean mValidateAuthority;

    URL mAuthorityUrl;
    boolean mIsTenantless;
    String mAuthorizationEndpoint;
    String mTokenEndpoint;
    AuthorityType mAuthorityType;
    /**
     * True if the authority is already validated, which basically means that we have already done authority validation.
     */
    boolean mIsAuthorityValidated = false;

    /**
     * Perform instance discovery to get the tenant discovery endpoint. If it's a valid authority url, tenant discovery
     * endpoint will be return, otherwise exception will be thrown.
     *
     * @param requestContext The {@link RequestContext} for the instance discovery request.
     * @return The tenant discovery endpoint.
     * @throws MsalException if error happens during the instance discovery.
     */
    abstract String performInstanceDiscovery(final RequestContext requestContext,
                                             final String userPrincipalName)
            throws MsalClientException, MsalServiceException;

    /**
     * @return True if the authority is already validated.
     */
    abstract boolean existsInResolvedAuthorityCache(final String userPrincipalName);

    /**
     * Adds this AuthorityMetadata to the {@link AuthorityMetadata#RESOLVED_AUTHORITY} cache.
     *
     * @param userPrincipalName the UPN of the current user (if available)
     */
    abstract void addToResolvedAuthorityCache(final String userPrincipalName);

    /**
     * Create the detailed authority. If the authority url string is for AAD, will create the {@link AadAuthorityMetadata}, otherwise
     * ADFS or B2C authority will be created.
     *
     * @param authorityUrl      The authority url used to create the {@link AuthorityMetadata}.
     * @param validateAuthority True if performing authority validation, false otherwise.
     * @return The {@link AuthorityMetadata} instance.
     */
    static AuthorityMetadata createAuthority(final String authorityUrl, final boolean validateAuthority) {
        final URL authority;
        try {
            authority = new URL(authorityUrl);
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException("malformed authority url.", e);
        }

        if (!HTTPS_PROTOCOL.equalsIgnoreCase(authority.getProtocol())) {
            throw new IllegalArgumentException("Invalid protocol for the authority url.");
        }

        if (MsalUtils.isEmpty(authority.getPath().replace("/", ""))) {
            throw new IllegalArgumentException("Invalid authority url");
        }

        final String[] pathSegments = authority.getPath().replaceFirst("/", "").split("/");
        final boolean isAdfsAuthority = pathSegments[0].equals(ADFS_AUTHORITY_PREFIX);
        final boolean isB2cAuthority = pathSegments[0].equals(B2C_AUTHORITY_PREFIX);

        if (isAdfsAuthority) {
            Logger.error(TAG, null, "ADFS authority is not a supported authority instance", null);
            throw new IllegalArgumentException("ADFS authority is not a supported authority instance");
        } else if (isB2cAuthority) {
            Logger.info(TAG, null, "Passed in authority string is a b2c authority, create a new b2c authority instance.");
            return new B2CAuthorityMetadata(authority, validateAuthority);
        }

        Logger.info(TAG, null, "Passed in authority string is an aad authority, create a new aad authority instance.");
        return new AadAuthorityMetadata(authority, validateAuthority);
    }

    /**
     * Convenience method for {@link AuthorityMetadata#getAuthorityHost()}. Constructs an AuthorityMetadata and returns the host portion of the URL.
     *
     * @param authorityUrl      The authority url used to create the {@link AuthorityMetadata}.
     * @param validateAuthority True if performing authority validation, false otherwise.
     * @return The host portion of the {@link AuthorityMetadata} instance's URL.
     */
    static String getAuthorityHost(final String authorityUrl, final boolean validateAuthority) {
        return createAuthority(authorityUrl, validateAuthority).getAuthorityHost();
    }

    /**
     * Perform authority validation and tenant discovery. If authority validation is done successfully, the tenant discovery
     * endpoint will be returned otherwise exception will be thrown. Returned tenant discovery endpoint will be used for
     * tenant discovery to get authorize and token endpoint. Developer could turn off authority validation, but for all the
     * authority, we'll do tenant discovery.
     *
     * @param requestContext {@link RequestContext} for the authority validation and tenant discovery.
     * @throws MsalException If error happens during authority or tenant discovery.
     */
    void resolveEndpoints(final RequestContext requestContext, final String userPrincipalName) throws MsalClientException, MsalServiceException {
        Logger.info(TAG, requestContext, "Perform authority validation and tenant discovery.");
        if (existsInResolvedAuthorityCache(userPrincipalName)) {
            Logger.info(TAG, requestContext, "AuthorityMetadata has already been resolved. ");

            final AuthorityMetadata preValidatedAuthority = RESOLVED_AUTHORITY.get(mAuthorityUrl.toString());
            if (!mValidateAuthority || preValidatedAuthority.mIsAuthorityValidated) {
                mAuthorizationEndpoint = preValidatedAuthority.mAuthorizationEndpoint;
                mTokenEndpoint = preValidatedAuthority.mTokenEndpoint;
                return;
            } else {
                Logger.info(TAG, requestContext, "AuthorityMetadata has not been validated, need to perform authority validation first.");
            }
        }

        final TenantDiscoveryResponse tenantDiscoveryResponse;
        final String openIdConfigurationEndpoint = performInstanceDiscovery(requestContext, userPrincipalName);
        try {
            final Oauth2Client oauth2Client = new Oauth2Client(requestContext);
            oauth2Client.addHeader(OauthConstants.OauthHeader.CORRELATION_ID, requestContext.getCorrelationId().toString());
            tenantDiscoveryResponse = oauth2Client.discoverEndpoints(new URL(openIdConfigurationEndpoint));
        } catch (final IOException ioException) {
            throw new MsalClientException(MsalClientException.IO_ERROR, ioException.getMessage(), ioException);
        }

        if (MsalUtils.isEmpty(tenantDiscoveryResponse.getAuthorizationEndpoint())
                || MsalUtils.isEmpty(tenantDiscoveryResponse.getTokenEndpoint())) {
            if (tenantDiscoveryResponse.getError() != null) {
                throw new MsalServiceException(tenantDiscoveryResponse.getError(), tenantDiscoveryResponse.getErrorDescription(),
                        tenantDiscoveryResponse.getHttpStatusCode(), null);
            }

            throw new MsalServiceException(MsalServiceException.UNKNOWN_ERROR, "Didn't receive either success or failure response from server",
                    tenantDiscoveryResponse.getHttpStatusCode(), null);
        }

        mAuthorizationEndpoint = tenantDiscoveryResponse.getAuthorizationEndpoint();
        mTokenEndpoint = tenantDiscoveryResponse.getTokenEndpoint();

        addToResolvedAuthorityCache(userPrincipalName);
    }

    /**
     * Constructor for the {@link AuthorityMetadata}.
     *
     * @param authorityUrl      The string representation for the authority url.
     * @param validateAuthority True if authority validation is set to be true, false otherwise.
     */
    protected AuthorityMetadata(final URL authorityUrl, final boolean validateAuthority) {
        mAuthorityUrl = updateAuthority(authorityUrl);
        mValidateAuthority = validateAuthority;

        // default value for tenant less is false. B2c and Adfs authority will never be tenant less.
        mIsTenantless = isTenantLess();
    }

    URL getAuthorityUrl() {
        return mAuthorityUrl;
    }

    /**
     * @return The String value for authority url.
     */
    String getAuthority() {
        return mAuthorityUrl.toString();
    }

    AuthorityType getAuthorityType() {
        return mAuthorityType;
    }

    String getAuthorityHost() {
        return mAuthorityUrl.getHost();
    }

    /**
     * @return The default openid configuration endpoint. If authority validation is turned off or the authority is in the
     * trusted authority list, the default openid configuration endpoint will be used to perform tenant discovery.
     */
    String getDefaultOpenIdConfigurationEndpoint() {
        return mAuthorityUrl.toString() + DEFAULT_OPENID_CONFIGURATION_ENDPOINT;
    }

    /**
     * @return Authorize endpoint.
     */
    String getAuthorizeEndpoint() {
        return mAuthorizationEndpoint;
    }

    /**
     * @return Token endpoint.
     */
    String getTokenEndpoint() {
        return mTokenEndpoint;
    }

    protected URL updateAuthority(final URL authority) {
        final String path = authority.getPath().replaceFirst("/", "");
        int indexOfSecondPath = path.indexOf("/");
        final String firstPath = path.substring(0, indexOfSecondPath == -1 ? path.length() : indexOfSecondPath);
        final String updatedAuthorityUrl = String.format("https://%s/%s", authority.getAuthority(), firstPath);
        final URL updatedAuthority;
        try {
            updatedAuthority = new URL(updatedAuthorityUrl);
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException("Malformed updated authority url.", e);
        }

        return updatedAuthority;
    }

    boolean getIsTenantless() {
        return mIsTenantless;
    }

    void updateTenantLessAuthority(final String tenantId) throws MsalClientException {
        if (!mIsTenantless || MsalUtils.isEmpty(tenantId)) {
            return;
        }

        final List<String> tenantLessNameList = Arrays.asList(TENANTLESS_TENANT_NAME);
        String authorityString = mAuthorityUrl.toString();
        for (final String name : tenantLessNameList) {
            authorityString = authorityString.replace(name, tenantId);
        }

        try {
            mAuthorityUrl = new URL(authorityString);
            mIsTenantless = false;
        } catch (final MalformedURLException e) {
            throw new MsalClientException(MsalClientException.MALFORMED_URL, "Fail to update tenant id for tenant less authority, ", e);
        }
    }

    private boolean isTenantLess() {
        final String[] pathSegments = mAuthorityUrl.getPath().replaceFirst("/", "").split("/");
        final String tenant = pathSegments[0];
        final List<String> tenantLessNames = Arrays.asList(TENANTLESS_TENANT_NAME);
        if (tenantLessNames.contains(tenant)) {
            return true;
        }

        return false;
    }

    /**
     * The AuthorityMetadata type.
     */
    enum AuthorityType {
        /**
         * AuthorityMetadata is an instance of AAD authority.
         */

        AAD,
        /**
         * AuthorityMetadata is an instance of ADFS authority.
         */
        ADFS,

        /**
         * AuthorityMetadata is an instance of B2C authority.
         */
        B2C
    }
}