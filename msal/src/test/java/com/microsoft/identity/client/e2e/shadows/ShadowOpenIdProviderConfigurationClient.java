package com.microsoft.identity.client.e2e.shadows;

import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdProviderConfiguration;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdProviderConfigurationClient;

import org.robolectric.annotation.Implements;

@Implements(OpenIdProviderConfigurationClient.class)
public class ShadowOpenIdProviderConfigurationClient {

    public synchronized OpenIdProviderConfiguration loadOpenIdProviderConfiguration() throws ServiceException {
        throw new ServiceException(
                "503",
                "Not allowed to query well known config for mocked tests",
                null);
    }
}
