/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.microsoft.identity.client.msal.automationapp.sdk;

public class Constants {
    /**
     * AT-PoP strings.
     */
    public static final String PoP_DOMAIN = "signedhttprequest.azurewebsites.net";

    public static final String PoP_URL_PATH = "/api/validateSHR";

    public static final String PoP_FULL_URL = "https://signedhttprequest.azurewebsites.net/api/validateSHR";

    public static final String HTTP_GET_METHOD = "GET";

    public static final String TOKEN_URL = "u";

    public static final String TOKEN_URL_PATH = "p";

    public static final String TOKEN_HTTP_METHOD = "m";

    public static final boolean IS_EXPECTING_SECOND_SPEED_BUMP = false;
}
