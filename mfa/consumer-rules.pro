# Copyright (c) Microsoft Corporation. All rights reserved.
# Licensed under the MIT License.

# The SDK's persisted state is serialized by Gson.
-keepclassmembers class com.microsoft.identity.mfa.internal.MfaStoredState** { <fields>; }
-keepattributes Signature
