//   Copyright (c) Microsoft Corporation.
//   All rights reserved.
//
//   This code is licensed under the MIT License.
//
//   Permission is hereby granted, free of charge, to any person obtaining a copy
//   of this software and associated documentation files(the "Software"), to deal
//   in the Software without restriction, including without limitation the rights
//   to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//   copies of the Software, and to permit persons to whom the Software is
//   furnished to do so, subject to the following conditions :
//
//   The above copyright notice and this permission notice shall be included in
//   all copies or substantial portions of the Software.
//
//   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//   THE SOFTWARE.

package com.microsoft.identity.client;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * MSAL internal class for representing the B2C authority instance.
 */
final class B2cAuthority extends AadAuthority {
    private static final String TAG = B2cAuthority.class.getSimpleName();
    private static int B2C_AUTHORITY_SEGMENTS_SIZE = 3;

    B2cAuthority(final URL authority, boolean validateAuthority) {
        super(authority, validateAuthority);

        mAuthorityType = AuthorityType.B2C;
    }

    /**
     * B2C authority will be in the format of https://<host>/tfp/<tenant>/<policy>/...
     * @param authority The passed in B2C authority
     * @return updated authority with only host/tfp/tenant/policy
     */
    @Override
    protected URL updateAuthority(final URL authority) {
        final String path = authority.getPath().replaceFirst("/", "");
        final String[] pathSegments = path.split("/");

        // b2c authority is in the format of https://<host>/tfp/<tenant>/<policy>/. A valid authority path has to have tfb,
        // tenant and policy.
        if (pathSegments == null || pathSegments.length < B2C_AUTHORITY_SEGMENTS_SIZE) {
            throw new IllegalArgumentException("Invalid B2C authority");
        }

        final URL updateAuthority;
        try {
            // constructing the expected b2c format with the passed in authority instance. Developer may pass more path in the
            // authority instance, we need trim in this case, only take in the host and the first three path segments.
            updateAuthority = new URL(String.format("https://%s/%s/%s/%s", authority.getAuthority(), pathSegments[0], pathSegments[1],
                    pathSegments[2]));
        } catch (final MalformedURLException e) {
            Logger.error(TAG, null, "Malformed authority url", e);
            throw new IllegalArgumentException("Malformed updated authority Url", e);
        }

        return updateAuthority;
    }

    @Override
    String performInstanceDiscovery(final RequestContext requestContext, final String userPrincipalName) throws MsalClientException {
        if (mValidateAuthority && !TRUSTED_HOST_SET.contains(mAuthorityUrl.getAuthority())) {
            // we don't support b2c authority validation for BUILD.
            Logger.error(TAG, null, "Authority validation is not supported for b2c authority.", null);
            throw new MsalClientException(MsalClientException.AUTHORITY_VALIDATION_NOT_SUPPORTED, "Authority validation cannot be done against B2c instance.");
        }

        return getDefaultOpenIdConfigurationEndpoint();
    }
}
