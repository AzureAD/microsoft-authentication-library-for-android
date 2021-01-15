package com.microsoft.identity.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.identity.client.internal.configuration.LogLevelDeserializer;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AuthorityDeserializer;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudienceDeserializer;

import org.junit.Assert;
import org.junit.Test;

public class PublicClientApplicationConfigurationTest {

    @Test
    public void testBadConfiguration() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(
                        Authority.class,
                        new AuthorityDeserializer()
                )
                .registerTypeAdapter(
                        AzureActiveDirectoryAudience.class,
                        new AzureActiveDirectoryAudienceDeserializer()
                )
                .registerTypeAdapter(
                        Logger.LogLevel.class,
                        new LogLevelDeserializer()
                )
                .create();

        String config = "{"
            + "\"client_id\": \"id\","
                + "\"authorization_user_agent\": \"WEBVIEW\","
                + "\"redirect_ur\": \"msauth://example.mobile.debug/EH9x23KWgdgd0w8lSiTahV6Q%2F%2FH95\","
                + "\"broker_redirect_uri_registered\": true,"
                + "\"shared_device_mode_supported\": false,"
                + "\"account_mode\": \"MULTIPLE\","
        + "\"authorities\": ["
            + "{"
                + "\"type\": \"B2C\","
                    + "\"authority_url\": \"https://example.b2clogin.com/tfp/example.onmicrosoft.com/B2C_1A_Signup_Signin_With_Kmsi\","
                    + "\"default\": true"
            + "}"
 + "]"
        + "}";
        PublicClientApplicationConfiguration conf = gson.fromJson(config, PublicClientApplicationConfiguration.class);
    }
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
