package com.microsoft.identity.client;

import org.junit.Assert;
import org.junit.Test;

public class PublicClientApplicationConfigurationTest {
    @Test
    public void testRedirectUriValidationValid() {
        Assert.assertTrue(PublicClientApplicationConfiguration.isBrokerRedirectUri("msauth://myPackageName/foo.bar/baz", "myPackageName"));
    }
    @Test
    public void testRedirectUriValidationInvalid() {
        Assert.assertFalse(PublicClientApplicationConfiguration.isBrokerRedirectUri("https://myPackageName/foo.bar/baz", "myPackageName"));
    }
    @Test
    public void testRedirectUriValidationWrongPackage() {
        Assert.assertFalse(PublicClientApplicationConfiguration.isBrokerRedirectUri("msauth://myPackageName/foo.bar/baz", "notMyPackageName"));
    }
}
