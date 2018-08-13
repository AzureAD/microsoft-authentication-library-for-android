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
 * MSAL API surface implementation of an account.
 */
class Account implements IAccount {

    private IAccountId mAccountId;
    private IAccountId mHomeAccountId;
    private String mUsername;

    Account() {
        // Empty constructor
    }

    /**
     * Sets the account id.
     *
     * @param accountId The IAccountId to set.
     */
    void setAccountId(final IAccountId accountId) {
        mAccountId = accountId;
    }

    @Override
    public IAccountId getAccountId() {
        return mAccountId;
    }

    /**
     * Sets the home account id.
     *
     * @param homeAccountId The IAccountId to set.
     */
    void setHomeAccountId(final IAccountId homeAccountId) {
        mHomeAccountId = homeAccountId;
    }

    @Override
    public IAccountId getHomeAccountId() {
        return mHomeAccountId;
    }

    /**
     * Sets the username.
     *
     * @param username The username to set.
     */
    void setUsername(final String username) {
        mUsername = username;
    }

    @Override
    public String getUsername() {
        return mUsername;
    }

}
