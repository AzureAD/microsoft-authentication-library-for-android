package com.microsoft.identity.client.parameters;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.claims.ClaimsRequest;

import java.util.List;

public abstract class TokenParameters {
    
    private List<String> mScopes;
    private IAccount mAccount;
    private String mAuthority;
    private ClaimsRequest mClaimsRequest;
    private AuthenticationCallback mCallback;


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
}
