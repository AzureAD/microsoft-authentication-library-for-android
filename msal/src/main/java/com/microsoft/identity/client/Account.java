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

    private IAccountIdentifier mAccountIdentifier;
    private IAccountIdentifier mHomeAccountIdentifier;
    private String mUsername;
    private String mEnvironment;

    Account() {
        // Empty constructor
    }

    /**
     * Sets the account id.
     *
     * @param accountId The IAccountIdentifier to set.
     */
    void setAccountIdentifier(final IAccountIdentifier accountId) {
        mAccountIdentifier = accountId;
    }

    @Override
    public IAccountIdentifier getAccountIdentifier() {
        return mAccountIdentifier;
    }

    /**
     * Sets the home account id.
     *
     * @param homeAccountId The IAccountIdentifier to set.
     */
    void setHomeAccountIdentifier(final IAccountIdentifier homeAccountId) {
        mHomeAccountIdentifier = homeAccountId;
    }

    @Override
    public IAccountIdentifier getHomeAccountIdentifier() {
        return mHomeAccountIdentifier;
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

    /**
     * Sets the environment.
     *
     * @param environment The environment to set.
     */
    void setEnvironment(final String environment) {
        mEnvironment = environment;
    }

    @Override
    public String getEnvironment() {
        return mEnvironment;
    }
}
