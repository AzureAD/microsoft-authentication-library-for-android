package com.microsoft.identity.client;

import java.util.ArrayList;

import com.microsoft.identity.client.authorities.Authority;
import com.microsoft.identity.client.authorities.AuthorityType;

public class KnownAuthorities extends ArrayList<Authority> {

    public boolean isKnown(Authority authority){
        return this.contains(authority);
    }

    public boolean isKnown(String authority){
        for (Authority a: this ){
            if(authority.equalsIgnoreCase(a.getAuthorityUri().toString())){
                return true;
            }
        }
        return false;
    }


    public void tryValidateAuthority(String authorityUri, AuthorityType type, AuthorityValidationCallback callback) {



    }

    public void tryValidateAuthority(Authority authority){

    }

}
