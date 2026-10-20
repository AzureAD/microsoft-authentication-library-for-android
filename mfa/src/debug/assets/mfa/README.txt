Copyright (c) Microsoft Corporation. All rights reserved.
Licensed under the MIT License.

Synthetic fixtures are constructed by FixtureMfaTransport through MfaXml rather than duplicated
literal XML assets. All IDs, proof secrets, tokens and accounts are fictional. They cannot enroll
against a live service. The fixture transport never delegates to a real HTTP client.

Flow: initialize -> register -> explicitly injectValidation -> registered -> injectChallenge ->
enter tester-only signInNumber -> approve. Refresh creates a fresh synthetic challenge.
Wrong number returns numeric failure 7; expired decisions return 8. Replays remain blocked.
