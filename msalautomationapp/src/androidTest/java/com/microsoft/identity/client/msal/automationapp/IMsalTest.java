//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client.msal.automationapp;

import com.microsoft.identity.client.ui.automation.browser.IBrowser;

/**
 * An interface describing a test of the Microsoft Authentication Library (MSAL). Implementing this
 * interface on a test facilitates specifying the necessary parameters required to test the basic
 * functionality in MSAL.
 */
public interface IMsalTest {

    /**
     * Get the scopes that can be used for an acquire token test.
     *
     * @return A string array consisting of OAUTH2 Scopes
     */
    String[] getScopes();

    /**
     * Get the authority url that can be used for an acquire token test.
     *
     * @return A string representing the url for an authority that can be used as token issuer
     */
    String getAuthority();

    /**
     * Get the browser that may be being used during an acquire test. If a broker is present on the
     * device then the browser may not be used for those acquire token requests.
     *
     * @return A {@link IBrowser} object representing the Android app of the browser being used
     */
    IBrowser getBrowser();

    /**
     * The MSAL config file that should be used to create a PublicClientApplication for the test.
     *
     * @return config file resource id
     */
    int getConfigFileResourceId();
}
