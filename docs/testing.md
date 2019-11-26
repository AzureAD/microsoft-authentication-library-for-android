# Testing

## Introduction

The MSAL Library contains both local unit tests that run inside the JVM, as well as instrumented tests that can be run on a real Android device or an emulator.
The instrumented tests are located inside the `androidTest` directory whereas the JVM based unit tests are located inside the `test` directory.

The `test` directory contains both unit tests that test a specific feature or method, as well as integration and end-to-end tests.
Some of the tests use the `JUnit` runner whereas the others use the `RobolectricTestRunner` as appropriate based on the nature of the tests.

All the End-to-End test are written using `Robolectric` framework which uses the `RobolectricTestRunner`. The e2e tests are located inside the `com.microsoft.identity.client.e2e.tests` package.

## Class Diagram

> PENDING

## Interaction Diagram

> PENDING

## Links to Code

> PENDING