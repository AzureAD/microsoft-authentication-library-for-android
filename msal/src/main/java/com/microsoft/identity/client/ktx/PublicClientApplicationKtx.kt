package com.microsoft.identity.client.ktx

import android.content.Context
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.IPublicClientApplication.IMultipleAccountApplicationCreatedListener
import com.microsoft.identity.client.IPublicClientApplication.ISingleAccountApplicationCreatedListener
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalClientException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


object PublicClientApplicationKtx {
    /**
     * PublicClientApplicationKtx.createSingleAccountPublicClientApplication will read
     * the client id and other configuration settings from the file included in your
     * application's resources.
     *
     * This function will throw an [MsalClientException] if it is unable to return an
     * [ISingleAccountPublicClientApplication]. For example, if AccountMode in the configuration
     * is not set to single.
     *
     * @param context Application's [Context].
     * The SDK requires the application context to be passed in to [PublicClientApplication].
     * Note: The [Context] should be the application context instead of the running activity's
     * context, which could potentially make the SDK hold a strong reference to the activity,
     * thus preventing correct garbage collection and causing bugs.
     * @param configFileResourceId The resource ID of the raw file containing the JSON
     * configuration for the PublicClientApplication.
     * For more information on the schema of the MSAL config json,
     * please see [Android app resource overview](https://developer.android.com/guide/topics/resources/providing-resources)
     * and [MSAL Github Wiki](https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki)
     * @see PublicClientApplication.createSingleAccountPublicClientApplication
     */
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

    /**
     * PublicClientApplicationKtx.createMultipleAccountPublicClientApplication will read
     * the client id and other configuration settings from the file included in your
     * application's resources.
     *
     * This function will throw an [MsalClientException] if it is unable to return an
     * [IMultipleAccountPublicClientApplication]. For example, when the device is marked
     * as 'shared' ([PublicClientApplication.isSharedDevice] is true).
     *
     * @param context Application's [Context].
     * The SDK requires the application context to be passed in to [PublicClientApplication].
     * Note: The [Context] should be the application context instead of the running activity's
     * context, which could potentially make the SDK hold a strong reference to the activity,
     * thus preventing correct garbage collection and causing bugs.
     * @param configFileResourceId The resource ID of the raw file containing the JSON
     * configuration for the PublicClientApplication.
     * For more information on the schema of the MSAL config json,
     * please see [Android app resource overview](https://developer.android.com/guide/topics/resources/providing-resources)
     * and [MSAL Github Wiki](https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki)
     * @see PublicClientApplication.createMultipleAccountPublicClientApplication
     */
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
