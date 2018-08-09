package com.microsoft.identity.client;

public interface IAccount {

    IAccountId getAccountId();

    IAccountId getHomeAccountId();

    String getUsername();

    boolean isCredentialPresent();

}
