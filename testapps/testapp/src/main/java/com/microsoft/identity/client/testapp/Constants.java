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
package com.microsoft.identity.client.testapp;

/**
 * Constants file.
 */

public class Constants {

    enum ConfigFile {
        DEFAULT,
        BROWSER,
        WEBVIEW,
        FAIRFAX,
        ARLINGTON,
        MOONCAKE,
        BLACKFOREST,
        INSTANCE_AWARE_COMMON,
        INSTANCE_AWARE_COMMON_SKIP_BROKER,
        INSTANCE_AWARE_ORGANIZATION,
        B2C,
        MSA,
        MSA_ONLY,
        NO_ADMIN_CONSENT,
        CIAM,
        PKEY_AUTH_SILENT,

        BROWSER_SKIP_BROKER,

        WEBVIEW_SKIP_BROKER

    }

    public static int getResourceIdFromConfigFile(ConfigFile configFile) {
        switch (configFile) {
            case BROWSER:
                return R.raw.msal_config_browser;

            case WEBVIEW:
                return R.raw.msal_config_webview;

            case FAIRFAX:
                return R.raw.msal_config_fairfax;

            case ARLINGTON:
                return R.raw.msal_config_arlington;

            case MOONCAKE:
                return R.raw.msal_config_mooncake;

            case BLACKFOREST:
                return R.raw.msal_config_blackforest;

            case INSTANCE_AWARE_COMMON:
                return R.raw.msal_config_instance_aware_common;

            case INSTANCE_AWARE_COMMON_SKIP_BROKER:
                return R.raw.msal_config_instance_aware_common_skip_broker;

            case INSTANCE_AWARE_ORGANIZATION:
                return R.raw.msal_config_instance_aware_organization;

            case B2C:
                return R.raw.msal_config_b2c;

            case MSA:
                return R.raw.msal_config_msa;

            case MSA_ONLY:
                return R.raw.msal_config_msa_only;

            case NO_ADMIN_CONSENT:
                return R.raw.msal_config_no_admin_consent;

            case CIAM:
                return R.raw.msal_config_ciam;

            case PKEY_AUTH_SILENT:
                return R.raw.msal_config_pkey_auth_silent;

            case BROWSER_SKIP_BROKER:
                return R.raw.msal_config_browser_skip_broker;

            case WEBVIEW_SKIP_BROKER:
                return R.raw.msal_config_webview_skip_broker;
        }

        return R.raw.msal_config_default;
    }

    public enum AuthScheme {
        BEARER,
        POP
    }

    public static final String STATE = "state";
    public static final String CODE_LENGTH = "code_length";
    public static final String SENT_TO = "sent_to";
    public static final String CHANNEL = "channel";
}
