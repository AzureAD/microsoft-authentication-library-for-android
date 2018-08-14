package com.microsoft.identity.client;

public interface AuthorityValidationCallback {
    /**
     * Authority validation finishes successfully.
     *
     * @param knownAuthority {@link Authority} that contains the success response.
     */
    void onSuccess(Authority knownAuthority);

    /**
     * Error occurs during the authentication.
     *
     * @param exception The {@link MsalException} contains the error code, error message and cause if applicable. The exception
     *                  returned in the callback could be {@link MsalClientException}, {@link MsalServiceException} or
     *                  {@link MsalUiRequiredException}.
     */
    void onError(final MsalException exception);
}
