# Controllers

## Introduction

> PENDING

## Class Diagram

> PENDING

## Interaction Diagram

> PENDING

## Native Auth V2 interaction routing

After authorize-challenge, every V2 interaction uses Common's
`NativeAuthV2ResponseParser.parseInteraction` and `NativeAuthV2FlowController.routeInteraction`.
Commands share `NativeAuthV2InteractionCommandResult`; entry points and states map it through
MSAL's `mapNativeAuthV2Result`, without operation-specific success allowlists.

The current action, authentication factor, and newest continuation links determine the next
state. Single-factor challenges are ordinary primary challenges regardless of the operation or
originating scenario; they do not reclassify sign-up as sign-in. Sign-up and password-reset completion still require explicit sign-in; reset completion
also requires a submitted password update. Authorize-challenge and token exchange remain separate.

Activation requires an email/SMS channel in the current server response; prior selected-method
metadata is not used as a fallback. `phone` alone is a resource type, not a channel. MFA
classification is retained when later responses omit the factor. There is no global automatic-transition cap; the existing
five-poll budget remains. Risk verification is no longer part of the V2 protocol.
Password submission uses the current action: `collectAttributes` submits a password attribute,
and `verify` verifies a password, regardless of the originating flow. The public password state
always sends the same submit-password command; Common's controller chooses the request using the
action retained in the internal continuation. Malformed responses and unsupported actions remain
errors. Public V2 method signatures, callbacks, and result classes are unchanged.

## Links to Code

> PENDING
