// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.e2e.utils;

public class TestConstants {

    public static class Configurations {
        public static final String B2C_CONFIG_FILE_PATH = "src/test/res/raw/b2c_test_config.json";
        public static final String MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH = "src/test/res/raw/multiple_account_aad_test_config.json";
        public static final String SINGLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH = "src/test/res/raw/single_account_aad_test_config.json";
    }

    public static class Scopes {
        public static final String[] USER_READ_SCOPE = {"user.read"};
        public static final String[] MS_GRAPH_USER_READ_SCOPE = {"https://graph.microsoft.com/user.read"};
        public static final String[] AD_GRAPH_USER_READ_SCOPE = {"https://graph.windows.net/user.read"};
        public static final String[] OFFICE_USER_READ_SCOPE = {"https://outlook.office.com/user.read"};
        public static final String[] B2C_SCOPE = {"https://msidlabb2c.onmicrosoft.com/msidlabb2capi/read"};
    }

    public static class Authorities {
        public static final String AAD_MOCK_AUTHORITY = "https://test.authority/mock";
        public static final String AAD_MOCK_DELAYED_RESPONSE_AUTHORITY = "https://test.authority/mock_with_delays";
    }

}
