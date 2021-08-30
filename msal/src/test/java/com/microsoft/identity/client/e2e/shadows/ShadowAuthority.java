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

import com.microsoft.identity.common.java.authorities.AccountsInOneOrganization;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.java.authorities.UnknownAuthority;
import com.microsoft.identity.internal.testutils.TestConstants;
import com.microsoft.identity.internal.testutils.authorities.AADTestAuthority;
import com.microsoft.identity.internal.testutils.authorities.B2CTestAuthority;
import com.microsoft.identity.internal.testutils.authorities.MockAuthority;
import com.microsoft.identity.internal.testutils.authorities.MockDelayedResponseAuthority;

import org.robolectric.annotation.Implements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

// A Shadow is Robolectric's way of mocking code
// A shadow works in a similar way to method overriding
// Implementing a shadow for a class does not mean that we are shadowing the entire class,
// instead we are only shadowing the particular method that is implemented in the shadow
// so in this case, the only thing that we are shadowing is the getAuthorityFromAuthorityUrl method in the Authority class
@Implements(Authority.class)
public class ShadowAuthority {

    private static final String TAG = ShadowAuthority.class.getSimpleName();

    private static final String AAD_MOCK_PATH_SEGMENT = "mock";
    private static final String B2C_PATH_SEGMENT = "tfp";
    private static final String B2C_PATH_SEGMENT_ALT = "te";
    private static final String AAD_MOCK_DELAYED_PATH_SEGMENT = "mock_with_delays";

    /**
     * Returns an Authority based on an authority url.  This method works in similar way to the actual
     * method in the Authority class, except that over here we create and return test versions of the Authorities
     *
     * @param authorityUrl
     * @return
     * @throws MalformedURLException
     */
    public static Authority getAuthorityFromAuthorityUrl(String authorityUrl) {
        final String methodName = ":getAuthorityFromAuthorityUrl";
        URL authUrl;

        try {
            authUrl = new URL(authorityUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid authority URL");
        }

        final Uri authorityUri = Uri.parse(authUrl.toString());
        final List<String> pathSegments = authorityUri.getPathSegments();

        if (pathSegments.size() == 0) {
            return new UnknownAuthority();
        }

        Authority authority = null; // Our result object...

        String authorityType = pathSegments.get(0);

        switch (authorityType.toLowerCase()) {
            // For our test environment, authority could be a AAD, B2C or a mocked authority
            // For AAD and B2C, we create a test version of that authority that supports ROPC
            // more cases can be added here in the future
            case AAD_MOCK_PATH_SEGMENT:
                //Return new AAD MOCK Authority
                authority = new MockAuthority(new AccountsInOneOrganization(
                        TestConstants.Authorities.AAD_MOCK_AUTHORITY,
                        TestConstants.Authorities.AAD_MOCK_AUTHORITY_TENANT)
                );
                break;
            case AAD_MOCK_DELAYED_PATH_SEGMENT:
                authority = new MockDelayedResponseAuthority();
                break;
            case B2C_PATH_SEGMENT:
            case B2C_PATH_SEGMENT_ALT:
                //Return new B2C TEST Authority
                authority = new B2CTestAuthority(authorityUrl);
                break;
            default:
                // return new AAD Test Authority
                authority = createAadAuthority(authorityUri, pathSegments);
                break;
        }

        return authority;
    }

    /**
     * Similar to createAadAuthority method in {@link Authority} class
     *
     * @param authorityUri the uri from which to derive authority
     * @param pathSegments determines the audience
     * @return
     */
    private static Authority createAadAuthority(@NonNull final Uri authorityUri,
                                                @NonNull final List<String> pathSegments) {
        AzureActiveDirectoryAudience audience = AzureActiveDirectoryAudience.getAzureActiveDirectoryAudience(
                authorityUri.getScheme() + "://" + authorityUri.getHost(),
                getPathForRopc(pathSegments)
        );

        return new AADTestAuthority(audience);
    }

    /**
     * Get ROPC compatible audience from path segments.
     * Changes ALL / Consumers to Organizations as all/consumers don't support ropc
     * If specific tenant id is passed then just returns that tenant id as audience
     *
     * @param pathSegments
     * @return
     */
    private static String getPathForRopc(@NonNull final List<String> pathSegments) {
        final String providedPath = pathSegments.get(0);
        if (providedPath.equals(AzureActiveDirectoryAudience.ALL) ||
                providedPath.equals(AzureActiveDirectoryAudience.CONSUMERS)) {
            return AzureActiveDirectoryAudience.ORGANIZATIONS;
        } else {
            return providedPath;
        }
    }


}
