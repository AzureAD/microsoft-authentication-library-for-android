package com.microsoft.identity.client.e2e.shadows;

import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.client.exception.MsalClientException;

import org.robolectric.annotation.Implements;

@Implements(PublicClientApplicationConfiguration.class)
public class ShadowPublicClientApplicationConfiguration {

    public void validateRedirectUriAndIntentFilter() throws MsalClientException {
        // don't need to validate redirect uri as Robolectric's PackageInfo object does not have
        // the signature information
        return;
    }
}
