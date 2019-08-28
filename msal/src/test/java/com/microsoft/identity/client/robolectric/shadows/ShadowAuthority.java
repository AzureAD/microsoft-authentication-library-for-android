package com.microsoft.identity.client.robolectric.shadows;

import android.net.Uri;

import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.authorities.MockAuthority;
import com.microsoft.identity.common.internal.authorities.RopcTestAuthority;
import com.microsoft.identity.common.internal.authorities.UnknownAuthority;
import com.microsoft.identity.common.internal.logging.Logger;

import org.robolectric.annotation.Implements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Implements(Authority.class)
public class ShadowAuthority {

    private static final String TAG = ShadowAuthority.class.getSimpleName();

    private static final String AAD_ROPC_TEST_PATH_SEGMENT = "aad.ropc";
    private static final String AAD_MOCK_PATH_SEGMENT = "aad.mock";

    /**
     * Returns an Authority based on an authority url.  This method attempts to parse the URL and based on the contents of it
     * determine the authority type and tenantid associated with it.
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
            // more cases can be added here in the future
            case AAD_ROPC_TEST_PATH_SEGMENT:
                //Return new Ropc Test Authority
                Logger.verbose(
                        TAG + methodName,
                        "Authority type is AAD ROPC"
                );
                authority = new RopcTestAuthority();
                break;
            case AAD_MOCK_PATH_SEGMENT:
                //Return new Ropc Test Authority
                Logger.verbose(
                        TAG + methodName,
                        "Authority type is AAD MOCK"
                );
                authority = new MockAuthority();
                break;

            default:
                Logger.verbose(
                        TAG + methodName,
                        "Authority type default: AAD"
                );
                authority = new AzureActiveDirectoryAuthority();
                break;
        }

        return authority;
    }


}
