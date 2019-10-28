# Commands & Command Dispatcher

## Introduction

MSAL uses the model view controller pattern for separating the UX (public API service) from the business logic (controller) used to service the API call.  In addition MSAL wraps up each API call as a command in order to facilitate parallel execution and command result caching to ensure that clients are not in a position to accidentally hammer the service with too much load by making the same request over and over again.

## Class Diagrams

### Controllers

The MSAL controllers address two different scenarios:

- Broker Not Installed on Device or App Not Eligible to use broker
  - LocalMSALController
- Broker installed on device and app eligible to use broker
  - BrokerMsalController

>Note: In the case of silent requests it's possible for the command to attempt to leverage both controllers when attempting to service the request.

![Controller Class Diagram](https://www.lucidchart.com/publicSegments/view/8dd3ceb4-d209-4f23-9713-34ed80011cf6/image.png)

### Commands

The MSAL commands include the following:

- Controller or Controllers invoked to execute the command
- The parameters specific to the command
- The callback to notify of the command result
- An indicator of whether the command is eligible for caching

> The hashCode of the command is used from within the CommandResultCache.  Ensure that any changes to the attributes of the command object are reflected in the calculation of getHashCode

![Commands Class Diagram](https://www.lucidchart.com/publicSegments/view/5593d30e-e10e-4e4d-8c34-c69b3479a25f/image.png)

### Operation Parameters

Operation parameters are provided via the public API and are passed to the controller by the command.  These parameters inherit from OperationParameters; subtypes include:

- AcquireTokenSilentOperationParameters
- AcquireTokenInteractiveOperationParameters
  - This includes the Android UI specific parameters include Activity

![Operation Parameters Class Diagram](https://www.lucidchart.com/publicSegments/view/0aaf4dd3-b8ab-49cc-96e8-af9f977a545a/image.png)

### Command Dispatcher & Command Cache

The command dispatcher is responsible for:

- Determining if the command is already executing and throwing a duplicate command exception if so
- Executing the command in a specific thread pool appropriate for the type of command.  For example: Interactive commands are serialized.  Silent commands are run in parallel.
- Check a command result cache which keeps command results from prior executions for up to 30 seconds
- Cache command results
- Notify via the callback provided to the command the result of the command

![Command dispatcher class diagram](https://www.lucidchart.com/publicSegments/view/5895956d-b887-42dc-8603-26aff8f59489/image.png)

## Interaction Diagram

Below is an example of the public client application making a silent token request to the command dispatcher.

### Calling Dispatcher

![calling command dispatcher](https://www.lucidchart.com/publicSegments/view/01e3ec3a-6b84-4c1b-8c7d-49d02a892dca/image.png)

## Links to Code

### Classes

- [CommandDispatcher](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/blob/dev/common/src/main/java/com/microsoft/identity/common/internal/controllers/CommandDispatcher.java)
- [BaseCommand](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/blob/dev/common/src/main/java/com/microsoft/identity/common/internal/controllers/BaseCommand.java)
- [BaseController](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/blob/dev/common/src/main/java/com/microsoft/identity/common/internal/controllers/BaseController.java)
- [CommandResult](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/blob/dev/common/src/main/java/com/microsoft/identity/common/internal/controllers/CommandResult.java)
- [CommandResultCacheItem](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/blob/dev/common/src/main/java/com/microsoft/identity/common/internal/controllers/CommandResultCacheItem.java)
- [CommandResultCache](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/blob/dev/common/src/main/java/com/microsoft/identity/common/internal/controllers/CommandResultCache.java)
- [TokenCommand](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/blob/dev/common/src/main/java/com/microsoft/identity/common/internal/controllers/TokenCommand.java)
