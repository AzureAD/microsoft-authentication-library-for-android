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
        BROWSER_SKIP_BROKER,
        WEBVIEW,
        WEBVIEW_SKIP_BROKER,
        FAIRFAX,
        ARLINGTON,
        MOONCAKE,
        BLACKFOREST,
        INSTANCE_AWARE_COMMON,
        INSTANCE_AWARE_ORGANIZATION,
        B2C,
        MSA
    }

    public static int getResourceIdFromConfigFile(ConfigFile configFile){
        switch (configFile){
            case BROWSER:
                return R.raw.msal_config_browser;

            case BROWSER_SKIP_BROKER:
                return R.raw.msal_config_browser_skip_broker;

            case WEBVIEW:
                return R.raw.msal_config_webview;

            case WEBVIEW_SKIP_BROKER:
                return R.raw.msal_config_webview_skip_broker;

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

            case INSTANCE_AWARE_ORGANIZATION:
                return R.raw.msal_config_instance_aware_organization;

            case B2C:
                return R.raw.msal_config_b2c;

            case MSA:
                return R.raw.msal_config_msa;
        }

        return R.raw.msal_config_default;
    }

    public enum AuthScheme {
        BEARER,
        POP
    }
}
