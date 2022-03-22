// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client;

import android.app.Activity;

import androidx.annotation.Nullable;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;
import java.util.List;

/**
 * Encapsulates the parameters for calling {@link SingleAccountPublicClientApplication#signIn(SignInParameters)}.
 * Not a subclass of TokenParameters because it does not need fields such as Account, AccountRecord.
 *
 * <br>
 * Activity  -  Non-null {@link Activity} that is used as the parent activity for launching the {@link com.microsoft.identity.common.internal.providers.oauth2.AuthorizationActivity}.
 *
 * <br>
 * LoginHint -  Optional. If provided, will be used as the query parameter sent for authenticating the user,
 *              which will have the UPN pre-populated.
 *
 * <br>
 * Scopes    -  The non-null list of scopes to be consented to during sign in.
 *              MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
 *              The access token returned is for MS Graph and will allow you to query for additional information about the signed in account.
 *
 * <br>
 * Callback  -  {@link AuthenticationCallback} that is used to send the result back. The success result will be
 *              sent back via {@link AuthenticationCallback#onSuccess(IAuthenticationResult)}.
 *              Failure case will be sent back via AuthenticationCallback.onError(MsalException).
 *
 * <br>
 * Prompt    -  Optional. Indicates the type of user interaction that is required.
 *              If no argument is supplied the default behavior will be used (default is SELECT_ACCOUNT).
 */
@Builder(setterPrefix = "with")
@Data
public class SignInParameters {
    private @NonNull Activity activity;
    private @Nullable String loginHint;
    @Singular private @NonNull List<String> scopes;
    private @Nullable Prompt prompt;
    private @NonNull AuthenticationCallback callback;
}
