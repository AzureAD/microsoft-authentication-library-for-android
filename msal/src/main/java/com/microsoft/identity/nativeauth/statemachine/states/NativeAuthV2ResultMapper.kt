// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.nativeauth.statemachine.states

import com.microsoft.identity.client.AuthenticationResultAdapter
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2InteractionCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpCommandResult
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.errors.*
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.toListOfRequiredUserAttributeV2
import com.microsoft.identity.nativeauth.toListOfV2AuthMethods

/** Submission context changes error classification, never the set of accepted outcomes. */
internal enum class NativeAuthV2ResultOperation {
    INTERACTION, SUBMIT_CODE, SUBMIT_PASSWORD, SUBMIT_NEW_PASSWORD, SUBMIT_ATTRIBUTES, MFA, ENROLL
}

/** The sole Common-command to public V2 result mapper, used by entry points and every state. */
internal fun mapNativeAuthV2Result(
    result: NativeAuthV2InteractionCommandResult,
    scenario: NativeAuthFlowScenarioV2,
    config: NativeAuthPublicClientApplicationConfiguration,
    operation: NativeAuthV2ResultOperation = NativeAuthV2ResultOperation.INTERACTION
): NativeAuthResultV2 {
    fun codeErrorType() = if (operation == NativeAuthV2ResultOperation.MFA ||
        operation == NativeAuthV2ResultOperation.ENROLL) ErrorTypes.INVALID_CHALLENGE else ErrorTypes.INVALID_CODE
    return when (result) {
        is NativeAuthV2CommandResult.StrongAuthRegistrationRequired -> {
            val methods = result.authMethods.toListOfV2AuthMethods()
            NativeAuthResultV2.StrongAuthRegistrationRequired(
                StrongAuthRegistrationRequiredStateV2(result.correlationId, scenario, config, result.continuationState, methods),
                scenario, methods)
        }
        is NativeAuthV2CommandResult.StrongAuthVerificationRequired -> {
            val channel = result.challengeChannel?.lowercase()
            if (channel != "email" && channel != "sms") {
                NativeAuthErrorV2(errorType = ErrorTypes.INVALID_STATE,
                    errorMessage = "The activation response does not identify a supported verification channel.",
                    correlationId = result.correlationId, scenario = scenario)
            } else NativeAuthResultV2.StrongAuthVerificationRequired(
                StrongAuthVerificationRequiredStateV2(result.correlationId, scenario, config, result.continuationState, channel),
                scenario, result.codeLength, result.challengeTargetLabel, channel)
        }
        is NativeAuthV2CommandResult.AuthMethodSelectionRequired -> {
            NativeAuthResultV2.AuthMethodSelectionRequired(AuthMethodSelectionRequiredStateV2(
                result.continuationState, result.authMethods.toListOfV2AuthMethods(), config, scenario))
        }
        is NativeAuthV2CommandResult.ExistingAccountCodeRequired -> result.toExistingAccountResult(config)
        is NativeAuthV2CommandResult.ExistingAccountPasswordRequired -> result.toExistingAccountResult(config)
        is NativeAuthV2CommandResult.CodeRequired -> {
            if (result.continuationState.isMultiFactorAuthentication()) {
                NativeAuthResultV2.MFAVerificationRequired(
                    MFAVerificationRequiredStateV2(result.continuationState, scenario, config),
                    scenario, result.codeLength, result.challengeTargetLabel, result.challengeChannel)
            } else NativeAuthResultV2.CodeRequired(CodeRequiredStateV2(result.continuationState, scenario, config),
                scenario, result.codeLength, result.challengeTargetLabel, result.challengeChannel)
        }
        is NativeAuthV2CommandResult.MFAVerificationRequired -> {
            NativeAuthResultV2.MFAVerificationRequired(
                MFAVerificationRequiredStateV2(result.continuationState, scenario, config),
                scenario, result.codeLength, result.challengeTargetLabel, result.challengeChannel)
        }
        is NativeAuthV2CommandResult.PasswordRequired -> {
            NativeAuthResultV2.PasswordRequired(PasswordRequiredStateV2(result.continuationState, scenario, config), scenario)
        }
        is NativeAuthV2CommandResult.NewPasswordRequired -> {
            NativeAuthResultV2.NewPasswordRequired(NewPasswordRequiredStateV2(result.continuationState, scenario, config), scenario)
        }
        is NativeAuthV2CommandResult.AttributesRequired -> {
            NativeAuthResultV2.AttributesRequired(AttributesRequiredStateV2(result.continuationState, scenario, config),
                scenario, result.requiredAttributes.toListOfRequiredUserAttributeV2())
        }
        is NativeAuthV2CommandResult.SignInAfterSignUpRequired -> {
            NativeAuthResultV2.SignInAfterSignUpRequired(SignInAfterSignUpStateV2(result.continuationState, scenario, config), scenario)
        }
        is NativeAuthV2CommandResult.SignInAfterResetPasswordRequired -> {
            NativeAuthResultV2.SignInAfterResetPasswordRequired(SignInAfterResetPasswordStateV2(result.continuationState, scenario, config), scenario)
        }
        is NativeAuthV2CommandResult.Complete -> {
            val authenticationResult = result.authenticationResult
            if (authenticationResult == null) NativeAuthErrorV2(
                errorMessage = "Native Auth V2 flow completed but no authentication result was returned.",
                correlationId = result.correlationId, scenario = scenario)
            else NativeAuthResultV2.Complete(AccountState.createFromAuthenticationResult(
                AuthenticationResultAdapter.adapt(authenticationResult), result.correlationId, config), scenario)
        }
        is NativeAuthV2CommandResult.AttributesInvalid -> NativeAuthErrorV2.fromAttributeError(result, scenario)
        is SignUpCommandResult.InvalidAttributes -> NativeAuthErrorV2(
            errorType = SignUpErrorTypes.INVALID_ATTRIBUTES, error = result.error,
            errorMessage = result.errorDescription, correlationId = result.correlationId, scenario = scenario)
        is INativeAuthCommandResult.InvalidUsername -> NativeAuthErrorV2(
            errorType = ErrorTypes.INVALID_USERNAME, error = result.error, errorMessage = result.errorDescription,
            correlationId = result.correlationId, scenario = scenario, errorCodes = result.errorCodes,
            exception = result.exception, traceId = result.traceId, timestamp = result.timestamp)
        is NativeAuthV2CommandResult.UserNotFound -> NativeAuthErrorV2(
            errorType = ErrorTypes.USER_NOT_FOUND, error = result.error, errorMessage = result.errorDescription,
            correlationId = result.correlationId, scenario = scenario, errorCodes = result.errorCodes,
            subError = result.subError, attributeValidationDetails = result.attributeValidationDetails,
            traceId = result.traceId, timestamp = result.timestamp)
        is NativeAuthV2CommandResult.IncorrectCode -> NativeAuthErrorV2(
            errorType = codeErrorType(), error = result.error, errorMessage = result.errorDescription,
            correlationId = result.correlationId, scenario = scenario, errorCodes = result.errorCodes,
            subError = result.subError, attributeValidationDetails = result.attributeValidationDetails,
            traceId = result.traceId, timestamp = result.timestamp)
        is NativeAuthV2CommandResult.IncorrectPassword -> NativeAuthErrorV2(
            errorType = ErrorTypes.INVALID_PASSWORD, error = result.error, errorMessage = result.errorDescription,
            correlationId = result.correlationId, scenario = scenario, errorCodes = result.errorCodes,
            subError = result.subError, attributeValidationDetails = result.attributeValidationDetails,
            traceId = result.traceId, timestamp = result.timestamp)
        is NativeAuthV2CommandResult.PasswordNotAccepted -> NativeAuthErrorV2(
            errorType = ErrorTypes.INVALID_PASSWORD, error = result.error, errorMessage = result.errorDescription,
            correlationId = result.correlationId, scenario = scenario, errorCodes = result.errorCodes,
            subError = result.subError, attributeValidationDetails = result.attributeValidationDetails,
            traceId = result.traceId, timestamp = result.timestamp)
        is NativeAuthV2CommandResult.PasswordResetFailed -> NativeAuthErrorV2(
            errorType = ResetPasswordErrorTypes.PASSWORD_RESET_FAILED, error = result.error,
            errorMessage = result.errorDescription, correlationId = result.correlationId, scenario = scenario)
        is NativeAuthV2CommandResult.UserAlreadyExists -> NativeAuthErrorV2(
            error = result.error, errorMessage = result.errorDescription, correlationId = result.correlationId, scenario = scenario,
            errorCodes = result.errorCodes, subError = result.subError, attributeValidationDetails = result.attributeValidationDetails,
            traceId = result.traceId, timestamp = result.timestamp)
        is NativeAuthV2CommandResult.NotImplemented -> NativeAuthErrorV2(
            errorType = ErrorTypes.NOT_IMPLEMENTED, error = result.error, errorMessage = result.errorDescription,
            correlationId = result.correlationId, scenario = scenario)
        is INativeAuthCommandResult.Redirect -> NativeAuthErrorV2(
            errorType = ErrorTypes.BROWSER_REQUIRED, error = result.error, errorMessage = result.errorDescription,
            correlationId = result.correlationId, scenario = scenario, errorCodes = result.errorCodes,
            traceId = result.traceId, timestamp = result.timestamp)
        is INativeAuthCommandResult.APIError -> NativeAuthErrorV2(
            errorType = when {
                result.error == "invalid_state" -> ErrorTypes.INVALID_STATE
                result.error == "invalidGrant" && result.subError == "invalidOneTimeCode" -> codeErrorType()
                result.error == "invalidGrant" && result.subError == "invalidUserNameOrPassword" &&
                    operation == NativeAuthV2ResultOperation.SUBMIT_PASSWORD -> ErrorTypes.INVALID_PASSWORD
                else -> null
            }, error = result.error, errorMessage = result.errorDescription,
            correlationId = result.correlationId, scenario = scenario, errorCodes = result.errorCodes,
            subError = result.subError, exception = result.exception, attributeValidationDetails = result.attributeValidationDetails,
            traceId = result.traceId, timestamp = result.timestamp)
        else -> NativeAuthErrorV2(errorType = ErrorTypes.INVALID_STATE,
            errorMessage = "The command returned an unrecognized Native Auth V2 result.",
            correlationId = result.correlationId, scenario = scenario)
    }
}
