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
final class B2CAuthority extends AADAuthority {

     B2CAuthority(final URL authority, boolean validateAuthority) {
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

        if (pathSegments == null || pathSegments.length < 3) {
            throw new IllegalArgumentException("Invalid B2C authority");
        }

        final URL updateAuthority;
        try {
            updateAuthority = new URL(String.format("https://%s/%s/%s/%s", authority.getHost(), pathSegments[0], pathSegments[1],
                    pathSegments[2]));
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException("Malformed updated authority Url", e);
        }

        return updateAuthority;
    }

    @Override
    String performInstanceDiscovery(final RequestContext requestContext, final String userPrincipalName) throws AuthenticationException {
        if (mValidateAuthority && !TRUSTED_HOST_SET.contains(mAuthorityUrl.getAuthority())) {
            // we don't support b2c authority validation for BUILD.
            throw new AuthenticationException(MSALError.UNSUPPORTED_AUTHORITY_VALIDATION, "B2C authority is not supported for doing authority validation");
        }

        return getDefaultOpenIdConfigurationEndpoint();
    }
}
