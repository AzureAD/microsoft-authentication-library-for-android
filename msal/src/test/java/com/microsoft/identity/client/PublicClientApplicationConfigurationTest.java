package com.microsoft.identity.client;

import org.junit.Test;

public class PublicClientApplicationConfigurationTest {
    @Test
    public void testRedirectUriValidationValid() {
        PublicClientApplicationConfiguration.isBrokerRedirectUri("msauth://myPackageName/foo.bar/baz", "myPackageName");
    }
    @Test
    public void testRedirectUriValidationInvalid() {
        PublicClientApplicationConfiguration.isBrokerRedirectUri("https://myPackageName/foo.bar/baz", "myPackageName");
    }
    @Test
    public void testRedirectUriValidationWrongPackage() {
        PublicClientApplicationConfiguration.isBrokerRedirectUri("msauth://myPackageName/foo.bar/baz", "notMyPackageName");
    }
}
