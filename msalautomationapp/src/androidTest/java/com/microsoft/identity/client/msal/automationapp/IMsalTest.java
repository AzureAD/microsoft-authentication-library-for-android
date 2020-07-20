package com.microsoft.identity.client.msal.automationapp;

import com.microsoft.identity.client.ui.automation.app.IApp;
import com.microsoft.identity.client.ui.automation.browser.IBrowser;

public interface IMsalTest {

    /**
     * Get the scopes that can be used for an acquire token test
     *
     * @return A string array consisting of OAUTH2 Scopes
     */
    String[] getScopes();

    /**
     * Get the authority url that can be used for an acquire token test
     *
     * @return A string representing the url for an authority that can be used as token issuer
     */
    String getAuthority();

    /**
     * Get the browser that may be being used during an acquire test. If a broker is present on the
     * device then the browser may not be used for those acquire token requests.
     *
     * @return A {@link IApp} object representing the Android app of the browser being used
     */
    IBrowser getBrowser();

    int getConfigFileResourceId();

}
