// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.e2e.shadows;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.authorities.AccountsInOneOrganization;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.internal.testutils.TestConstants;
import com.microsoft.identity.internal.testutils.authorities.MockAuthorityHttpResponse;

import org.robolectric.annotation.Implements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * A Robolectric shadow for the {@link Authority} class. This shadow will always use the
 * {@link MockAuthorityHttpResponse} authority for token requests to allow using the mock token
 * responses during token requests.
 */
@Implements(Authority.class)
public class ShadowAuthorityForMockHttpResponse {

    /**
     * Returns an Authority based on an authority url.  This method works in similar way to the actual
     * method in the Authority class, except that over here we create and return test versions of the
     * authority
     *
     * @param authorityUrl
     * @return
     * @throws MalformedURLException
     */
    public static Authority getAuthorityFromAuthorityUrl(String authorityUrl) {
        URL authUrl;
        try {
            authUrl = new URL(authorityUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid authority URL");
        }

        final Uri authorityUri = Uri.parse(authUrl.toString());
        final List<String> pathSegments = authorityUri.getPathSegments();
        return createAadAuthority(authorityUri, pathSegments);
    }

    public static boolean isKnownAuthority(Authority authority) {
        return true;
    }

    private static Authority createAadAuthority(@NonNull final Uri authorityUri,
                                                @NonNull final List<String> pathSegments) {
        AzureActiveDirectoryAudience audience = AzureActiveDirectoryAudience.getAzureActiveDirectoryAudience(
                authorityUri.getScheme() + "://" + authorityUri.getHost(),
                pathSegments.get(0)
        );

        return new MockAuthorityHttpResponse(audience);
    }

}
