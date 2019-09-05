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

package com.microsoft.identity.client;

import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.internal.logging.Logger;

/**
 * The UI options that developer can pass during interactive token acquisition requests.
 */
public enum Prompt {



    /**
     * acquireToken will send prompt=select_account to the authorize endpoint. Shows a list of users from which can be
     * selected for authentication.
     */
    SELECT_ACCOUNT,

    /**
     * acquireToken will send prompt=login to the authorize endpoint.  The user will always be prompted for credentials by the service.
     * <p>
     * toString override is to enable the correct protocol value of login to be returned instead of "force_login".
     */
    LOGIN,

    /**
     * acquireToken will send prompt=consent to the authorize endpoint.  The user will be prompted to consent even if consent was granted before.
     */
    CONSENT,

    /**
     * acquireToken will not send the prompt parameter to the authorize endpoint.  The user may be prompted to login or to consent as required by the request.
     */
    WHEN_REQUIRED;

    @Override
    public String toString() {
        switch (this) {
            case SELECT_ACCOUNT:
                return SELECT_ACCOUNT.name().toLowerCase();
            case LOGIN:
                return LOGIN.name().toLowerCase();
            case CONSENT:
                return CONSENT.name().toLowerCase();
            case WHEN_REQUIRED:
                return WHEN_REQUIRED.name().toLowerCase();
            default:
                throw new IllegalArgumentException();
        }
    }

    public OpenIdConnectPromptParameter toOpenIdConnectPromptParameter() {
        String tag = Prompt.class.getSimpleName() + ":toOpenIdConnectPromptParameter";
        switch (this) {
            case SELECT_ACCOUNT:
                return OpenIdConnectPromptParameter.SELECT_ACCOUNT;
            case LOGIN:
                return OpenIdConnectPromptParameter.LOGIN;
            case CONSENT:
                return OpenIdConnectPromptParameter.CONSENT;
            case WHEN_REQUIRED:
                String error = "WHEN_REQUIRED Does not have corresponding value in in the OIDC prompt enumeration.  It's meant to convey do not sent the prompt parameter.";
                Logger.info(tag, error);
                throw new UnsupportedOperationException(error);
            default:
                return OpenIdConnectPromptParameter.SELECT_ACCOUNT;
        }
    }
}