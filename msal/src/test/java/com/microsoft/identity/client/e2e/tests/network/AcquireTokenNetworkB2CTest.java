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

import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import static com.microsoft.identity.client.e2e.utils.TestConstants.Configurations.B2C_CONFIG_FILE_PATH;
import static com.microsoft.identity.client.e2e.utils.TestConstants.Scopes.B2C_SCOPE;

/**
 * Run all tests in the {@link AcquireTokenNetworkTest} class using B2C
 */
public class AcquireTokenNetworkB2CTest extends AcquireTokenNetworkTest {

    @Override
    public LabUserQuery getLabUserQuery() {
        final LabUserQuery query = new LabUserQuery();
        query.userType = LabConstants.UserType.B2C;
        query.b2cProvider = LabConstants.B2CProvider.LOCAL;
        return query;
    }

    @Override
    public String getConfigFilePath() {
        return B2C_CONFIG_FILE_PATH;
    }

    @Override
    public String[] getScopes() {
        return B2C_SCOPE;
    }
}
