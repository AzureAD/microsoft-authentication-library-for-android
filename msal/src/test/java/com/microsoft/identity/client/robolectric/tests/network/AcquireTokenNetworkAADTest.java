package com.microsoft.identity.client.robolectric.tests.network;

/**
 * Run all tests in the {@link AcquireTokenNetworkTest} class using AAD
 */
public class AcquireTokenNetworkAADTest extends AcquireTokenNetworkTest {

    public AcquireTokenNetworkAADTest() {
        this.mAuthorityType = AAD_AUTHORITY_TYPE_STRING;
        this.mScopes = AAD_SCOPES;
    }
}
