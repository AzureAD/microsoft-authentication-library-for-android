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
 * Class holding the constants value.
 */
public class AutomationAppConstants {
    public static final String ACCESS_TOKEN = "access_token";
    public static final String ACCESS_TOKEN_TYPE = "access_token_type";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String EXPIRES_ON = "expires_on";
    public static final String TENANT_ID = "tenant_id";
    public static final String UNIQUE_ID = "unique_id";
    public static final String DISPLAYABLE_ID = "displayable_id";
    public static final String NAME = "name";
    public static final String FAMILY_NAME = "family_name";
    public static final String IDENTITY_PROVIDER = "identity_provider";
    public static final String ID_TOKEN = "id_token";
    public static final String UNIQUE_USER_IDENTIFIER = "unique_user_identifier";

    public static final String ERROR = "error";
    public static final String ERROR_DESCRIPTION = "error_description";
    public static final String ERROR_CAUSE = "error_cause";
    public static final String ERROR_HTTP_CODE = "error_http_code";

    public static final String READ_CACHE = "all_items";
    public static final String ITEM_COUNT = "item_count";
    public static final String EXPIRED_ACCESS_TOKEN_COUNT = "expired_access_token_count";
    public static final String INVALIDATED_REFRESH_TOKEN_COUNT = "invalidated_refresh_token_count";
    public static final String INVALIDATED_FAMILY_REFRESH_TOKEN_COUNT = "invalidated_family_refresh_token_count";
    public static final String CLEARED_ACCESS_TOKEN_COUNT = "cleared_access_token_count";
    public static final String CLEARED_REFRESH_TOKEN_COUNT= "cleared_refresh_token_count";
    public static final String READ_LOGS = "msal_logs";

    public static final String JSON_ERROR = "json_error";

    protected static class CACHE_DATA {
        static final String ACCESS_TOKEN = AutomationAppConstants.ACCESS_TOKEN;
        static final String REFRESH_TOKEN = AutomationAppConstants.REFRESH_TOKEN;
        static final String RESOURCE = "resource";
        static final String AUTHORITY = "authority";
        static final String CLIENT_ID = "client_id";
        static final String RAW_ID_TOKEN = "id_token";
        static final String EXPIRES_ON = "expires_on";
        static final String TENANT_ID = AutomationAppConstants.TENANT_ID;
        static final String UNIQUE_USER_ID = AutomationAppConstants.UNIQUE_ID;
        static final String DISPLAYABLE_ID = AutomationAppConstants.DISPLAYABLE_ID;
        static final String UNIQUE_USER_IDENTIFIER = AutomationAppConstants.UNIQUE_USER_IDENTIFIER;
        static final String FAMILY_NAME = AutomationAppConstants.FAMILY_NAME;
        static final String IDENTITY_PROVIDER = AutomationAppConstants.IDENTITY_PROVIDER;
    }
}
