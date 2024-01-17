package com.microsoft.identity.nativeauth.statemachine.results

import com.microsoft.identity.client.IAuthenticationResult

interface GetAccessTokenResult : Result {

    class Complete(override val resultValue: IAuthenticationResult) :
        Result.CompleteResult(resultValue = resultValue),
        GetAccessTokenResult
}
