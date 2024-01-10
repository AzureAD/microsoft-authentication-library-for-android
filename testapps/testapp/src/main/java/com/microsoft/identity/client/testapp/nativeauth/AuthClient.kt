package com.microsoft.identity.client.testapp.nativeauth

import android.app.Application
import android.content.Context
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.testapp.R
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication

object AuthClient : Application() {
    private lateinit var authClient: INativeAuthPublicClientApplication

    fun getAuthClient(): INativeAuthPublicClientApplication {
        return authClient
    }

    fun initialize(context: Context) {
        authClient = PublicClientApplication.createNativeAuthPublicClientApplication(
            context,
            R.raw.msal_config_native
        )
    }
}