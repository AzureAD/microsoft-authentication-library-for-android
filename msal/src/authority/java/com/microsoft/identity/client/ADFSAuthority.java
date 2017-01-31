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

import android.net.Uri;
import android.support.annotation.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * MSAL internal class for representing the ADFS authority.
 */
final class ADFSAuthority extends Authority {

    private static final int DELIM_NOT_FOUND = -1;

    private static final String UPN_DOMAIN_SUFFIX_DELIM = "@";

    private final Set<String> mADFSValidatedAuthorities =
            Collections.synchronizedSet(new HashSet<String>());

    /**
     * Constructor for the {@link Authority}.
     *
     * @param authorityUrl      The string representation for the authority url.
     * @param validateAuthority Validate authority before sending token request
     */
    protected ADFSAuthority(final URL authorityUrl, final boolean validateAuthority) {
        super(authorityUrl, validateAuthority);
        mAuthorityType = AuthorityType.ADFS;
    }

    @Override
    boolean existsInValidatedAuthorityCache(final String userPrincipalName) {
        if (MSALUtils.isEmpty(userPrincipalName)) {
            throw new IllegalArgumentException("userPrincipalName cannot be null or blank");
        }

        final Map<String, Authority> authorityMap = Authority.VALIDATED_AUTHORITY;

        return authorityMap.containsKey(mAuthorizationEndpoint)
                && authorityMap.get(mAuthorizationEndpoint) instanceof ADFSAuthority
                && ((ADFSAuthority) authorityMap.get(mAuthorizationEndpoint))
                .getADFSValidatedAuthorities()
                .contains(getDomainFromUPN(userPrincipalName));
    }

    @Override
    void addToValidatedAuthorityCache(final String userPrincipalName) {
        ADFSAuthority adfsInstance = this;

        if (Authority.VALIDATED_AUTHORITY.containsKey(mAuthorizationEndpoint)) {
            adfsInstance = (ADFSAuthority) VALIDATED_AUTHORITY.get(mAuthorizationEndpoint);
        }

        adfsInstance
                .getADFSValidatedAuthorities()
                .add(getDomainFromUPN(userPrincipalName));

        Authority.VALIDATED_AUTHORITY.put(mAuthorizationEndpoint, adfsInstance);
    }

    @Override
    String performInstanceDiscovery(final UUID correlationId, final String userPrincipalName) throws AuthenticationException {
        if (mValidateAuthority) {
            final DRSMetadata drsMetadata = loadDRSMetadata(correlationId, userPrincipalName);
            final WebFingerMetadata webFingerMetadata = loadWebFingerMetadata(correlationId, drsMetadata);

            final URI authorityURI;

            try {
                authorityURI = mAuthorityUrl.toURI();
            } catch (URISyntaxException e) {
                throw new AuthenticationException(
                        MSALError.AUTHORITY_VALIDATION_FAILED,
                        "Authority URL/URI must be RFC 2396 compliant to use AD FS validation"
                );
            }

            // Verify trust
            if (!ADFSWebFingerValidator.realmIsTrusted(authorityURI, webFingerMetadata)) {
                throw new AuthenticationException(MSALError.AUTHORITY_VALIDATION_FAILED);
            }
        }

        return getDefaultOpenIdConfigurationEndpoint();
    }

    public Set<String> getADFSValidatedAuthorities() {
        return mADFSValidatedAuthorities;
    }

    private WebFingerMetadata loadWebFingerMetadata(final UUID correlationId, final DRSMetadata drsMetadata) throws AuthenticationException {
        final WebFingerMetadataRequestor webFingerMetadataRequestor = new WebFingerMetadataRequestor();
        webFingerMetadataRequestor.setCorrelationId(correlationId);
        return webFingerMetadataRequestor.requestMetadata(
                new WebFingerMetadataRequestParameters(mAuthorityUrl, drsMetadata)
        );
    }

    private DRSMetadata loadDRSMetadata(final UUID correlationId, final String userPrincipalName) throws AuthenticationException {
        final DRSMetadataRequestor drsRequestor = new DRSMetadataRequestor();
        drsRequestor.setCorrelationId(correlationId);
        return drsRequestor.requestMetadata(getDomainFromUPN(userPrincipalName));
    }

    /**
     * From the supplied UPN, get the domain.
     *
     * @return the domain suffix of the UPN
     */
    @Nullable
    static String getDomainFromUPN(final String upn) {
        String suffix = null;
        if (upn != null) {
            final int dIndex = upn.lastIndexOf(UPN_DOMAIN_SUFFIX_DELIM);
            suffix = DELIM_NOT_FOUND == dIndex ? null : upn.substring(dIndex + 1);
        }
        return suffix;
    }

    @Override
    String getDefaultOpenIdConfigurationEndpoint() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(mAuthorityUrl.getAuthority())
                .appendPath("adfs")
                .appendPath(".well-known")
                .appendPath("openid-configuration");
        return builder.build().toString();
    }
}
