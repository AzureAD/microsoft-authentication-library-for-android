package com.microsoft.identity.client

import com.microsoft.identity.common.java.util.ObjectMapper

class UserAttributes(internal val userAttributes: Map<String, String>) {
    companion object Builder {
        private const val CITY = "city"
        private const val COUNTRY = "country"
        private const val DISPLAY_NAME = "displayName"
        private const val EMAIL_ADDRESS = "email"
        private const val GIVEN_NAME = "givenName"
        private const val JOB_TITLE = "jobTitle"
        private const val POSTAL_CODE = "postalCode"
        private const val STATE = "state"
        private const val STREET_ADDRESS = "streetAddress"
        private const val SURNAME = "surname"

        private val userAttributes = mutableMapOf<String, String>()

        fun city(city: String): Builder {
            userAttributes[CITY] = city
            return this
        }

        fun country(country: String): Builder {
            userAttributes[COUNTRY] = country
            return this
        }

        fun displayName(displayName: String): Builder {
            userAttributes[DISPLAY_NAME] = displayName
            return this
        }

        fun emailAddress(emailAddress: String): Builder {
            userAttributes[EMAIL_ADDRESS] = emailAddress
            return this
        }

        fun givenName(givenName: String): Builder {
            userAttributes[GIVEN_NAME] = givenName
            return this
        }

        fun jobTitle(jobTitle: String): Builder {
            userAttributes[JOB_TITLE] = jobTitle
            return this
        }

        fun postalCode(postalCode: String): Builder {
            userAttributes[POSTAL_CODE] = postalCode
            return this
        }

        fun state(state: String): Builder {
            userAttributes[STATE] = state
            return this
        }

        fun streetAddress(streetAddress: String): Builder {
            userAttributes[STREET_ADDRESS] = streetAddress
            return this
        }

        fun surname(surname: String): Builder {
            userAttributes[SURNAME] = surname
            return this
        }

        fun customAttribute(key: String, value: String): Builder {
            userAttributes[key] = value
            return this
        }

        fun build(): UserAttributes {
            return UserAttributes(userAttributes)
        }
    }
}

internal fun UserAttributes.toMap(): Map<String, String> {
    return ObjectMapper.constructMapFromObject(userAttributes)
}
