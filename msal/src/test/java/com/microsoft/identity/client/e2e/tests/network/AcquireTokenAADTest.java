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
package com.microsoft.identity.client.e2e.tests.network;

import com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import static com.microsoft.identity.internal.testutils.TestConstants.Configurations.MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH;
import static com.microsoft.identity.internal.testutils.TestConstants.Configurations.MULTIPLE_ACCOUNT_MODE_AAD_MOONCAKE_CONFIG_FILE_PATH;
import static com.microsoft.identity.internal.testutils.TestConstants.Scopes.AD_GRAPH_USER_READ_SCOPE;
import static com.microsoft.identity.internal.testutils.TestConstants.Scopes.OFFICE_USER_READ_SCOPE;
import static com.microsoft.identity.internal.testutils.TestConstants.Scopes.USER_READ_SCOPE;

/**
 * Run all tests in the {@link AcquireTokenNetworkTest} class using AAD
 */
public abstract class AcquireTokenAADTest extends AcquireTokenNetworkTest {

    @Override
    public String getConfigFilePath() {
        return MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH;
    }

    @Override
    public String[] getScopes() {
        return USER_READ_SCOPE;
    }

    @Override
    public String getAuthority() {
        return (String) AcquireTokenTestHelper.getAccount().getClaims().get("iss");
    }

    public static class AzureWorldWideCloudUser extends AcquireTokenAADTest {
        @Override
        public LabUserQuery getLabUserQuery() {
            final LabUserQuery query = new LabUserQuery();
            query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_CLOUD;
            return query;
        }
    }

    public static class AzureUsGovCloudUser extends AcquireTokenAADTest {
        @Override
        public LabUserQuery getLabUserQuery() {
            final LabUserQuery query = new LabUserQuery();
            query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_US_GOVERNMENT;
            return query;
        }
    }

    public static class AzureChinaCloudUser extends AcquireTokenAADTest {
        @Override
        public String getConfigFilePath() {
            return MULTIPLE_ACCOUNT_MODE_AAD_MOONCAKE_CONFIG_FILE_PATH;
        }

        @Override
        public LabUserQuery getLabUserQuery() {
            final LabUserQuery query = new LabUserQuery();
            query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_CHINA_CLOUD;
            return query;
        }
    }

    public static class AzureGermanyCloudUser extends AcquireTokenAADTest {
        @Override
        public LabUserQuery getLabUserQuery() {
            final LabUserQuery query = new LabUserQuery();
            query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_GERMANY_CLOUD;
            return query;
        }
    }

    public static class MamCaUserAadGraphResource extends AcquireTokenAADTest {
        @Override
        public String[] getScopes() {
            return AD_GRAPH_USER_READ_SCOPE;
        }

        @Override
        public LabUserQuery getLabUserQuery() {
            final LabUserQuery query = new LabUserQuery();
            query.protectionPolicy = LabConstants.ProtectionPolicy.MAM_CA;
            return query;
        }
    }

    public static class MamOnSpoUserOfficeResource extends AcquireTokenAADTest {
        @Override
        public String[] getScopes() {
            return OFFICE_USER_READ_SCOPE;
        }

        @Override
        public LabUserQuery getLabUserQuery() {
            final LabUserQuery query = new LabUserQuery();
            query.protectionPolicy = LabConstants.ProtectionPolicy.MAM_SPO;
            return query;
        }
    }
}
