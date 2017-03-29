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

package com.microsoft.identity.client;

/**
 * Internal interface for ApiEvent telemetry data.
 */
interface IApiEvent extends IEvent {

    /**
     * Gets the authority.
     *
     * @return the authority to get.
     */
    String getAuthority();

    /**
     * Gets the UI behavior.
     *
     * @return the UI behavior to get.
     */
    String getUiBehavior();

    /**
     * Gets the API Id.
     *
     * @return the API id to get.
     */
    String getApiId();

    /**
     * Gets the validation scheme.
     *
     * @return the validation scheme to get.
     */
    String getValidationStatus();

    /**
     * Gets the IDP name.
     *
     * @return the IDP name to get.
     */
    String getIdpName();

    /**
     * Gets the tenant id.
     *
     * @return the tenant id to get.
     */
    String getTenantId();

    /**
     * Gets the user id.
     *
     * @return the user id to get.
     */
    String getUserId();

    /**
     * Gets the loginHint.
     *
     * @return the loginHint to get.
     */
    String getLoginHint();

    /**
     * Gets the extended expires-on status.
     *
     * @return true if the status is extended, otherwise false.
     */
    Boolean getExtendedExpiresOnStatus();

    /**
     * Gets the success-status.
     *
     * @return true if the call was successful, otherwise false.
     */
    Boolean wasSuccessful();

}
