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
package com.microsoft.identity.client.msal.automationapp.sdk;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.ui.automation.sdk.AuthResult;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.providers.oauth2.IDToken;

import java.util.Map;

// MSAL Result Class to handle asserting success or failure on execution of Automated Test Cases
public class MsalAuthResult extends AuthResult {

    private Map<String, ?> claims;
    public static final String atPopSuccessMsg = "successfully verified at-pop";

    public MsalAuthResult(@NonNull final IAuthenticationResult authenticationResult) {
        super(authenticationResult.getAccessToken(), authenticationResult.getAccount().getIdToken(), authenticationResult.getAccount().getId(), authenticationResult.getAccount().getUsername(), authenticationResult.getAccount().getAuthority());
        this.claims = authenticationResult.getAccount().getClaims();
    }

    public MsalAuthResult(@NonNull final Exception exception) {
        super(exception);
    }

    public String verifyATForPop(@NonNull final String rawIdToken) throws ServiceException {
        Map<String, ?> tokens = IDToken.parseJWT(rawIdToken);

        // Verify if the url, path and http method are as expected
        if (!tokens.get("u").equals("signedhttprequest.azurewebsites.net"))
            return "Decoded AccessToken does not contain the PoPResourceUri";

        if (!tokens.get("p").equals("/api/validateSHR"))
            return "Decoded AccessToken does not contain the PoPResource path";

        if (!tokens.get("m").equals("GET"))
            return "Decoded AccessToken does not contain the expected HTTP method";

        return "successfully verified at-pop";
    }
}
