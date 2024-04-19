//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.nativeauth

import com.microsoft.identity.common.java.nativeauth.providers.responses.UserAttributeApiResult
import com.microsoft.identity.common.java.nativeauth.util.ILoggable

/**
 * RequiredUserAttribute represents details about the account attributes required by the server.
 */
data class RequiredUserAttribute(
    //Name of the attribute
    val attributeName: String?,

    //Data type for the attribute
    val type: String?,

    //If the attribute is required
    val required: Boolean?,

    //Attribute value should match the constraints
    val options: RequiredUserAttributeOptions?
) : ILoggable {
    override fun toUnsanitizedString(): String = "RequiredUserAttribute(attributeName=$attributeName, " +
            "type=$type, required=$required, options=$options)"

    override fun toString(): String = toUnsanitizedString()
}

/**
 * Converts a list of required user attribute API received as part of signup API to
 * a list of [RequiredUserAttribute] object
 */
internal fun List<UserAttributeApiResult>.toListOfRequiredUserAttribute(): List<RequiredUserAttribute> {
    return this.map { it.toRequiredUserAttribute() }
}

/**
 * Converts the required user attribute API received as part of signup API to
 * [RequiredUserAttribute] object
 */
internal fun UserAttributeApiResult.toRequiredUserAttribute(): RequiredUserAttribute {
    return RequiredUserAttribute(
        attributeName = this.name,
        type = this.type,
        required = this.required,
        options = this.options?.toListOfRequiredUserAttributeOptions()
    )
}
