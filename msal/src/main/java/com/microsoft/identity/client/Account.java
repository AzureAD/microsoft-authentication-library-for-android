package com.microsoft.identity.client;

public class Account implements IAccount {

    private IAccountId mAccountId;
    private IAccountId mHomeAccountId;
    private String mUsername;
    private boolean mCredentialPresent;

    Account() {
        // Empty constructor
    }

    void setAccountId(final IAccountId accountId) {
        mAccountId = accountId;
    }

    @Override
    public IAccountId getAccountId() {
        return mAccountId;
    }

    void setHomeAccountId(final IAccountId homeAccountId) {
        mHomeAccountId = homeAccountId;
    }

    @Override
    public IAccountId getHomeAccountId() {
        return mHomeAccountId;
    }

    void setUsername(final String username) {
        mUsername = username;
    }

    @Override
    public String getUsername() {
        return mUsername;
    }

    void setCredentialPresent(final boolean isPresent) {
        mCredentialPresent = isPresent;
    }

    @Override
    public boolean isCredentialPresent() {
        return mCredentialPresent;
    }
}
