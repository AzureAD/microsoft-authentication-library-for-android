package com.microsoft.identity.client

import com.microsoft.identity.common.java.util.ObjectMapper

/**
 * UserAttributes is a helper class for a client to provide user attributes required for signup
 * operation in Native Auth
 */
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

        /**
         * Sets the city for user
         * @param city: City for user
         */
        fun city(city: String): Builder {
            userAttributes[CITY] = city
            return this
        }

        /**
         * Sets the country for the user
         * @param country: Country for the user
         */
        fun country(country: String): Builder {
            userAttributes[COUNTRY] = country
            return this
        }

        /**
         * Sets the name for display purposes for the user
         * @param displayName: Display name for the user
         */
        fun displayName(displayName: String): Builder {
            userAttributes[DISPLAY_NAME] = displayName
            return this
        }

        /**
         * Sets the email address for the user
         * @param emailAddress: Email address for the user
         */
        fun emailAddress(emailAddress: String): Builder {
            userAttributes[EMAIL_ADDRESS] = emailAddress
            return this
        }

        /**
         * Sets the given name for the user
         * @param givenName: Given name for the user
         */
        fun givenName(givenName: String): Builder {
            userAttributes[GIVEN_NAME] = givenName
            return this
        }

        /**
         * Sets the job title for the user
         * @param givenName: Given name for the user
         */
        fun jobTitle(jobTitle: String): Builder {
            userAttributes[JOB_TITLE] = jobTitle
            return this
        }

        /**
         * Sets the given name for the user
         * @param givenName: Given name for the user
         */
        fun postalCode(postalCode: String): Builder {
            userAttributes[POSTAL_CODE] = postalCode
            return this
        }

        /**
         * Sets the state/province for the user
         * @param state: State/province for the user
         */
        fun state(state: String): Builder {
            userAttributes[STATE] = state
            return this
        }

        /**
         * Sets the street address for the user
         * @param streetAddress: Street address for the user
         */
        fun streetAddress(streetAddress: String): Builder {
            userAttributes[STREET_ADDRESS] = streetAddress
            return this
        }

        /**
         * Sets the surname for the user
         * @param surname: Surname for the user
         */
        fun surname(surname: String): Builder {
            userAttributes[SURNAME] = surname
            return this
        }

        /**
         * Sets any custom attribute for the use
         * @param key: Name of the attribute
         * @param value: Attribute value
         */
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
