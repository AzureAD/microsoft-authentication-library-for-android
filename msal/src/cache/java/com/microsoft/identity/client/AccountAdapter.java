package com.microsoft.identity.client;

import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;

class AccountAdapter {

    public static IAccount adapt(final Account accountIn) {
        final com.microsoft.identity.client.Account accountOut
                = new com.microsoft.identity.client.Account();

        // Populate fields...
        final IAccountIdentifier accountId;
        final IAccountIdentifier homeAccountId;

        if (MicrosoftAccount.AUTHORITY_TYPE_V1_V2.equals(accountIn.getAuthorityType())) {
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
        } else {
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

        return accountOut;
    }
}
