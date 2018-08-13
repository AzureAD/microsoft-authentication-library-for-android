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

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;

/**
 * MSAL internal class for constants.
 */
final class Constants {

    // Private constructor to prevent class from being initialized.
    private Constants() {
    }

    public static final String REQUEST_URL_KEY = "com.microsoft.identity.request.url.key";

    public static final String REQUEST_ID = AuthenticationConstants.Browser.REQUEST_ID;

    public static final String TELEMETRY_REQUEST_ID = "com.microsoft.identity.telemetry.request.id";

    public static final String CUSTOM_TAB_REDIRECT = "com.microsoft.identity.customtab.redirect";

    public static final String AUTHORIZATION_FINAL_URL = AuthenticationConstants.Browser.RESPONSE_FINAL_URL;

    public static final String WEBVIEW_SELECTION = "com.microsoft.identity.webview.selection";

    static final class UIResponse {
        static final int CANCEL = 2001;

        static final int AUTH_CODE_ERROR = 2002;

        static final int AUTH_CODE_COMPLETE = 2003;

        static final String ERROR_CODE = "error_code";

        static final String ERROR_DESCRIPTION = "error_description";
    }

    static final class MsalErrorMessage {
        static final String AUTHORIZATION_SERVER_INVALID_RESPONSE = "The authorization server returned an invalid "
                + "response.";

        static final String USER_CANCELLED_FLOW = "User pressed device back button to cancel the flow.";

        static final String STATE_NOT_THE_SAME = "Returned state from authorize endpoint is not the same as the one sent";

        static final String STATE_NOT_RETURNED = "State is not returned";
    }

    static final class MsalInternalError {
        static final String AUTHORIZATION_FAILED = "authorization_failed";
        static final String USER_CANCEL = "user_cancelled";
    }
}
