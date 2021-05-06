package com.microsoft.identity.client.stresstests;

public interface INotifyOperationResultCallback<T> {

    void onSuccess(T result);

    void onError(String message);
}
