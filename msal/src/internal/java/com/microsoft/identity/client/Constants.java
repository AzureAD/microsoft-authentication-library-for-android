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

/**
 * Created by weij on 8/11/2016.
 */
final class Constants {

    // Private constructor to prevent class from being initialized.
    private Constants() { }

    public static final String REQUEST_URL_KEY = "com.microsoft.identity.request.url.key";

    public static final String REQUEST_ID = "com.microsoft.identity.request.id";

    public static final String CUSTOM_TAB_REDIRECT = "com.microsoft.identity.customtab.redirect";

    static final class UIRequest {
        static final int BROWSER_FLOW = 1001;
    }

    static final class UIResponse {
        public static final int CANCEL = 2001;

        public static final int AUTH_CODE_ERROR = 2002;

        public static final int AUTH_CODE_COMPLETE = 2003;

        public static final int SUCCESS = 2004;
    }

    static final class MSALErrorMessage {
        static final String AUTHORIZATION_SERVER_INVALID_RESPONSE = "The authorization server returned an invalid "
                + "response.";
    }

    static final class MSALError {
        static final String AUTHORIZATION_FAILED = "authorization_failed";
    }
}
