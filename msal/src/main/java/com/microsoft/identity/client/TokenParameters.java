package com.microsoft.identity.client;

import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.common.internal.dto.AccountRecord;

import java.util.List;

abstract class TokenParameters {
    
    private List<String> mScopes;
    private IAccount mAccount;
    private String mAuthority;
    private ClaimsRequest mClaimsRequest;
    private AuthenticationCallback mCallback;
    private AccountRecord mAccountRecord;


    public List<String> getScopes() {
        return mScopes;
    }

    public void setScopes(List<String> scopes) {
        this.mScopes = scopes;
    }

    public IAccount getAccount() {
        return mAccount;
    }

    public void setAccount(IAccount account) {
        this.mAccount = account;
    }

    public String getAuthority() {
        return mAuthority;
    }

    public void setAuthority(String authority) {
        this.mAuthority = authority;
    }

    public ClaimsRequest getClaimsRequest() {
        return mClaimsRequest;
    }

    public void setClaimsRequest(ClaimsRequest claimsRequest) {
        this.mClaimsRequest = claimsRequest;
    }

    public AuthenticationCallback getCallback() {
        return mCallback;
    }

    public void setCallback(AuthenticationCallback callback) {
        this.mCallback = callback;
    }

    void setAccountRecord(AccountRecord record){
        mAccountRecord = record;
    }

    public AccountRecord getAccountRecord(){
        return mAccountRecord;
    }
}
