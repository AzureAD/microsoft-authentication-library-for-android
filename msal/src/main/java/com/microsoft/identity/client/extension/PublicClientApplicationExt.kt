package com.microsoft.identity.client.extension

import android.content.Context
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.IPublicClientApplication.IMultipleAccountApplicationCreatedListener
import com.microsoft.identity.client.IPublicClientApplication.ISingleAccountApplicationCreatedListener
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


object PublicClientApplicationExt {
    suspend fun createSingleAccountPublicClientApplication(
        context: Context,
        configFileResourceId: Int,
    ): ISingleAccountPublicClientApplication =
        suspendCancellableCoroutine { continuation ->
            PublicClientApplication.createSingleAccountPublicClientApplication(
                /* context = */ context,
                /* configFileResourceId = */ configFileResourceId,
                /* listener = */ object : ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        continuation.resume(application)
                    }

                    override fun onError(exception: MsalException) {
                        continuation.cancel(exception)
                    }
                }
            )
        }

    suspend fun createMultipleAccountPublicClientApplication(
        context: Context,
        configFileResourceId: Int,
    ): IMultipleAccountPublicClientApplication =
        suspendCancellableCoroutine { continuation ->
            PublicClientApplication.createMultipleAccountPublicClientApplication(
                /* context = */ context,
                /* configFileResourceId = */ configFileResourceId,
                /* listener = */ object : IMultipleAccountApplicationCreatedListener {
                    override fun onCreated(application: IMultipleAccountPublicClientApplication) {
                        continuation.resume(application)
                    }

                    override fun onError(exception: MsalException) {
                        continuation.cancel(exception)
                    }
                }
            )
        }
}
