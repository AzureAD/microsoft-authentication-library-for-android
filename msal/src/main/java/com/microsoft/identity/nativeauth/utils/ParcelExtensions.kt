package com.microsoft.identity.nativeauth.utils

import android.os.Build
import android.os.Parcel
import java.io.Serializable

/**
 * Helper method to deal with Parcel.readSerializable being deprecated in newer Android versions.
 */
inline fun <reified T : Serializable> Parcel.serializable(): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> readSerializable(T::class.java.classLoader, T::class.java)
    else -> @Suppress("DEPRECATION") readSerializable() as? T
}