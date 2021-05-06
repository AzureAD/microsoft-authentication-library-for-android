package com.microsoft.identity.client.stresstests;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(prefix = "m")
@AllArgsConstructor
public class AsyncResult<T> {
    private final T mResult;
    private final boolean mSuccess;
}
