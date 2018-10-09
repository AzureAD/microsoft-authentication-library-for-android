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
package com.microsoft.identity.client;

import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;

/**
 * Adapter class for Account transformations.
 */
class AccountAdapter {

    private static final String TAG = AccountAdapter.class.getSimpleName();

    /**
     * Adapts the {@link AccountRecord} type to the MSAL-exposed {@link IAccount}.
     *
     * @param accountIn The Account to transform.
     * @return A representation of the supplied Account, as an IAccount.
     */
    static IAccount adapt(final AccountRecord accountIn) {
        final String methodName = ":adapt";
        final com.microsoft.identity.client.Account accountOut
                = new com.microsoft.identity.client.Account();

        // Populate fields...
        final IAccountIdentifier accountId;
        final IAccountIdentifier homeAccountId;

        if (MicrosoftAccount.AUTHORITY_TYPE_V1_V2.equals(accountIn.getAuthorityType())) {
            Logger.info(
                    TAG + methodName,
                    "Account type is AAD"
            );

            // This account came from AAD
            accountId = new AzureActiveDirectoryAccountIdentifier() {{ // This is the local_account_id
                setIdentifier(accountIn.getLocalAccountId());
                setObjectIdentifier(accountIn.getLocalAccountId());
                setTenantIdentifier(accountIn.getRealm()); // TODO verify this is the proper field...
            }};
            homeAccountId = new AzureActiveDirectoryAccountIdentifier() {{ // This is the home_account_id
                // Grab the homeAccountId
                final String homeAccountIdStr = accountIn.getHomeAccountId();

                // Split it into its constituent pieces <uid>.<utid>
                final String[] components = homeAccountIdStr.split("\\.");

                // Set the full string value as the identifier
                setIdentifier(homeAccountIdStr);

                // Set the uid as the objectId
                setObjectIdentifier(components[0]);

                // Set the utid as the tenantId
                setTenantIdentifier(components[1]);
            }};
        } else { // This Account came from IdP other than AAD.
            Logger.info(
                    TAG + methodName,
                    "Account is non-AAD"
            );
            accountId = new AccountIdentifier() {{
                setIdentifier(accountIn.getLocalAccountId());
            }};
            homeAccountId = new AccountIdentifier() {{
                setIdentifier(accountIn.getHomeAccountId());
            }};
        }

        accountOut.setAccountIdentifier(accountId);
        accountOut.setHomeAccountIdentifier(homeAccountId);
        accountOut.setUsername(accountIn.getUsername());
        accountOut.setEnvironment(accountIn.getEnvironment());

        Logger.verbosePII(
                TAG + methodName,
                "Username: [" + accountIn.getUsername() + "]"
        );

        Logger.verbosePII(
                TAG + methodName,
                "Environment: [" + accountIn.getEnvironment() + "]"
        );

        return accountOut;
    }
}
