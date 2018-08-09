package com.microsoft.identity.client;

public abstract class AccountId implements IAccountId {

    private String mIdentifier;

    void setIdentifier(final String identifier) {
        mIdentifier = identifier;
    }

    @Override
    public String getIdentifier() {
        return mIdentifier;
    }
}
