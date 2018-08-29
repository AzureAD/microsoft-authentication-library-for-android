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

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Validates trusts between authorities and ADFS instances using DRS metadata and WebFinger.
 */
final class AdfsWebFingerValidator {

    private AdfsWebFingerValidator() {
        // utility class
    }

    /**
     * Used for logging.
     */
    private static final String TAG = AdfsWebFingerValidator.class.getSimpleName();

    /**
     * Constant identifying trust between two realms.
     */
    private static final URI TRUSTED_REALM_REL;

    static {
        try {
            TRUSTED_REALM_REL = new URI("http://schemas.microsoft.com/rel/trusted-realm");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Verify that trust is established between IDP and the SP.
     *
     * @param authority the endpoint used
     * @param metadata  the {@link WebFingerMetadata} to consult
     * @return True, if trust exists: otherwise false.
     */
    static boolean realmIsTrusted(final RequestContext requestContext, final URI authority, final WebFingerMetadata metadata) {
        if (authority == null) {
            throw new IllegalArgumentException("AuthorityMetadata cannot be null");
        }

        if (metadata == null) {
            throw new IllegalArgumentException("WebFingerMetadata cannot be null");
        }

        Logger.verbosePII(TAG, requestContext, "Verifying trust: " + authority.toString() + metadata.toString());

        if (metadata.getLinks() != null) {
            for (Link link : metadata.getLinks()) {
                try {
                    URI href = new URI(link.getHref());
                    URI rel = new URI(link.getRel());
                    if (href.getScheme().equalsIgnoreCase(authority.getScheme())
                            && href.getAuthority().equalsIgnoreCase(authority.getAuthority())
                            && rel.equals(TRUSTED_REALM_REL)) {
                        return true;
                    }
                } catch (URISyntaxException e) {
                    // noop
                    continue;
                }
            }
        }
        return false;
    }

}
