package com.microsoft.identity.client.msal.automationapp.web;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.UiResponse;
import com.microsoft.identity.client.msal.automationapp.broker.ITestBroker;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Getter
public class PromptHandlerParameters {

    @NonNull
    private Prompt prompt;
    private boolean sessionExpected;
    private boolean loginHintProvided;
    private boolean consentPageExpected;
    private boolean speedBumpExpected;
    private boolean expectingNonZeroAccountsInBroker;
    private boolean expectingNonZeroAccountsInCookie;

    @Builder.Default
    private UiResponse consentPageResponse = UiResponse.ACCEPT;

    @Builder.Default
    private UiResponse speedBumpResponse = UiResponse.ACCEPT;

    private ITestBroker broker;
}
