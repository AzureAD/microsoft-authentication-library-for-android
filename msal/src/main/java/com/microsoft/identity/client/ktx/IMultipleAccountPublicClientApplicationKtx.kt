package com.microsoft.identity.client.ktx

import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication.RemoveAccountCallback
import com.microsoft.identity.client.IPublicClientApplication.LoadAccountsCallback
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


suspend fun IMultipleAccountPublicClientApplication.getAccountsSuspend(): List<IAccount>? =
    suspendCancellableCoroutine { continuation ->
        this.getAccounts(object : LoadAccountsCallback {
            override fun onTaskCompleted(result: List<IAccount>?) {
                continuation.resume(result)
            }

            override fun onError(exception: MsalException) {
                continuation.cancel(exception)
            }
        })
    }

suspend fun IMultipleAccountPublicClientApplication.removeAccountSuspend(account: IAccount) =
    suspendCancellableCoroutine { continuation ->
        this.removeAccount(account, object : RemoveAccountCallback {
            override fun onRemoved() {
                continuation.resume(Unit)
            }

            override fun onError(exception: MsalException) {
                continuation.cancel(exception)
            }
        })
    }
