package com.microsoft.identity.client;

import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PromptTest {

    private Prompt prompt;

    @Test
    public void testOpenIdConnectParameterSelectAccount() {
        prompt = Prompt.SELECT_ACCOUNT;
        final OpenIdConnectPromptParameter promptValue = prompt.toOpenIdConnectPromptParameter();
        Assert.assertEquals(promptValue, OpenIdConnectPromptParameter.SELECT_ACCOUNT);
    }

    @Test
    public void testOpenIdConnectParameterLogin() {
        prompt = Prompt.LOGIN;
        final OpenIdConnectPromptParameter promptValue = prompt.toOpenIdConnectPromptParameter();
        Assert.assertEquals(promptValue, OpenIdConnectPromptParameter.LOGIN);
    }

    @Test
    public void testOpenIdConnectParameterConsent() {
        prompt = Prompt.CONSENT;
        final OpenIdConnectPromptParameter promptValue = prompt.toOpenIdConnectPromptParameter();
        Assert.assertEquals(promptValue, OpenIdConnectPromptParameter.CONSENT);
    }

    @Test
    public void testOpenIdConnectParameterWhenRequired() {
        prompt = Prompt.WHEN_REQUIRED;
        final OpenIdConnectPromptParameter promptValue = prompt.toOpenIdConnectPromptParameter();
        Assert.assertEquals(promptValue, OpenIdConnectPromptParameter.NOT_SENT);
    }


}
