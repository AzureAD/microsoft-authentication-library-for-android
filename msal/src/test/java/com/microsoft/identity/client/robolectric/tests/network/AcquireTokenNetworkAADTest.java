package com.microsoft.identity.client.robolectric.tests.network;

public class AcquireTokenNetworkAADTest extends AcquireTokenNetworkTest {

    public AcquireTokenNetworkAADTest() {
        this.mAuthorityType = AAD_AUTHORITY_TYPE_STRING;
        this.mScopes = AAD_SCOPES;
    }
}
