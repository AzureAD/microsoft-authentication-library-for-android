package com.microsoft.identity.client.statemachine.results

import com.microsoft.identity.client.statemachine.states.AccountState

interface GetAccountResult : Result {

    class AccountFound(override val resultValue: AccountState) :
        Result.CompleteResult(resultValue = resultValue),
        GetAccountResult

    object NoAccountFound :
        Result.CompleteResult(),
        GetAccountResult
}
