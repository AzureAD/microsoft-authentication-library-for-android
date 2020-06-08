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
package com.microsoft.identity.client.msal.automationapp;

import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

/**
 * An interface describing basic requirements to write a network (LAB API) based acquire token
 * e2e test.
 */
public interface IAcquireTokenNetworkTest extends IAcquireTokenTest {

    /**
     * Get the query that can be used to pull a user from the LAB API
     *
     * @return A {@link LabUserQuery} object that can be used to pull user via LAB API
     */
    LabUserQuery getLabUserQuery();

    /**
     * Get the type of temp user that can be used to create a new temp user via LAB API
     *
     * @return The type of temp user as denoted in {@link com.microsoft.identity.internal.testutils.labutils.LabConstants.TempUserType}
     */
    String getTempUserType();

}
