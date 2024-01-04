package com.microsoft.identity.nativeauth.statemachine.results

import com.microsoft.identity.nativeauth.statemachine.states.AccountState

interface GetAccountResult : Result {

    class AccountFound(override val resultValue: AccountState) :
        Result.CompleteResult(resultValue = resultValue),
        GetAccountResult

    object NoAccountFound :
        Result.CompleteResult(),
        GetAccountResult
}
