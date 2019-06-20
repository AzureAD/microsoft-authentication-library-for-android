package com.microsoft.identity.client.internal;

import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;

public class CreateApplicationResult  {

    private MsalException mMsalException;
    private IPublicClientApplication mIPublicClientApplication;
    private Boolean mSuccess = false;

    public CreateApplicationResult(IPublicClientApplication application, MsalException exception){
        mMsalException = exception;
        mIPublicClientApplication = application;
        if(application != null){
            mSuccess = true;
        }
    }

    public IPublicClientApplication getPublicClientApplication(){
        return mIPublicClientApplication;
    }

    public MsalException getException(){
        return mMsalException;
    }

    public Boolean getSuccess(){
        return mSuccess;
    }

}
