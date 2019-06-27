package com.microsoft.identity.client.internal;

import android.support.annotation.Nullable;

import com.microsoft.identity.client.exception.MsalException;

public class AsyncResult<T> {
    private T mResult;
    private MsalException mMsalException;
    private boolean mSuccess = false;

    public AsyncResult(@Nullable T result, @Nullable MsalException exception){
        mResult = result;
        mMsalException = exception;

        if(result != null){
            mSuccess = true;
        }
    }

    public T getResult(){
        return mResult;
    }

    public MsalException getException(){
        return mMsalException;
    }

    public boolean getSuccess(){
        return mSuccess;
    }
}
