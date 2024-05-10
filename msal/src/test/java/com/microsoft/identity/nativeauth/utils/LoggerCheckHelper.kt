package com.microsoft.identity.nativeauth.utils

import com.microsoft.identity.client.ILoggerCallback
import com.microsoft.identity.client.Logger
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class LoggerCheckHelper(private val externalLogger: ILoggerCallback, private val allowPII: Boolean) {

    private val sensitivePIIMessages = listOf(
        """(?<![\[\(])["]password["][:=]?(?![\]\)\}])""",  // '"password":' '"password"=' exclude 'password' '"challengeType":["password"]' '"challenge_type":"password"}'
        """(?<![\s\?\(])(code)[:=]""",  // 'code:' 'code=' exclude 'codeLength' 'error?code'
        """(?<![\(])continuationToken[:=]""",
        """(?<![\(])attributes[:=]""",
        """(?i)\b(accessToken|access_token)[:=]""", // access_token, accessToken
        """(?i)\b(refreshToken|refresh_token)[:=]""",
        """(?i)\b(idToken|id_token)[:=]""",
        """(?i)\b(continuation_token)[:=]""",
        """^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$""" // JWT token
    )
    private val permittedPIIMessages = listOf(
        """(?<![\(])username[:=]""",
        """(?i)\b(challengeTargetLabel|challenge_target_label)[:=]"""
    )

    init {
        setupLogger()
    }

    private fun setupLogger() {
        Logger.getInstance().setLogLevel(Logger.LogLevel.INFO)
        Logger.getInstance().setEnablePII(allowPII)
        Logger.getInstance().setExternalLogger(externalLogger)
    }

    private fun clearLogger() {
        Logger.getInstance().removeExternalLogger()
    }

    fun checkSafeLogging() {
        var allowList = listOf<String>()
        val disableList: List<String>

        if (allowPII) {
            allowList = permittedPIIMessages
            disableList = sensitivePIIMessages
        } else {
            disableList = sensitivePIIMessages + permittedPIIMessages
        }

        allowList.forEach { regex ->
            verifyLogCouldContain(regex)
        }
        disableList.forEach { regex ->
            verifyLogDoesNotContain(regex)
        }

        clearLogger()
    }

    private fun verifyLogDoesNotContain(regex: String) {
        verify(externalLogger, never()).log(
            any(),
            any(),
            argThat(RegexMatcher(regex)),
            ArgumentMatchers.anyBoolean()
        )
    }

    private fun verifyLogCouldContain(regex: String) {
        verify(externalLogger, never()).log(
            any(),
            any(),
            argThat(RegexMatcher(regex)),  // allowList items are logged but the containsPII should be true.
            eq(false)
        )
    }


    class RegexMatcher(private val regex: String) : ArgumentMatcher<String> {
        override fun matches(argument: String?): Boolean {
            return regex.toRegex().containsMatchIn(argument ?: "")
        }
    }
}