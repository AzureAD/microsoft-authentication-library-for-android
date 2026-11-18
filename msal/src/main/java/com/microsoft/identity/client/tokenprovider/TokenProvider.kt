// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.client.tokenprovider

/**
 * Host-supplied delegated token acquisition, independent of the consuming feature.
 *
 * Implementations may use native authentication or browser-based MSAL. Sharing a provider does not
 * make tokens interchangeable: honor each request's authority, account, scopes and claims in both
 * silent and interactive acquisition. Fail explicitly when requirements cannot be satisfied; never
 * discard claims or substitute an account. Marshal UI work to the main thread and do not present UI
 * when [TokenProviderParameters.allowsInteraction] is false.
 *
 * The host owns authentication fallback and provider error types. Consuming features propagate
 * provider-thrown errors unchanged without wrapping, classifying or logging them, including explicit
 * sign-in cancellation. Coroutine cancellation also propagates unchanged. Validation of a
 * successfully returned result is instead owned by the consuming SDK feature.
 *
 * No native/browser adapter or token acquisition is implemented by this API-review contract.
 */
interface TokenProvider {
    /**
     * Acquires a token satisfying [parameters] and returns actual authenticated-account metadata.
     * Never log the request or result. Throw instead of returning a partial or fabricated result.
     */
    suspend fun getAccessToken(parameters: TokenProviderParameters): TokenProviderResult
}
