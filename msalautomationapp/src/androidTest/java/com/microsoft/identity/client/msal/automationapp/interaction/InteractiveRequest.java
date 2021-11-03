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
package com.microsoft.identity.client.msal.automationapp.interaction;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.IPublicClientApplication;

/**
 * This class is a wrapper around the {@link IPublicClientApplication#acquireToken(AcquireTokenParameters)}
 * method in MSAL. The wrapper makes it easy (and required) to specify a handler for processing
 * user interaction in an interactive acquire token test.
 */
public class InteractiveRequest {

    private IPublicClientApplication application;
    private AcquireTokenParameters parameters;
    private OnInteractionRequired interactionRequiredCallback;

    public InteractiveRequest(
            @NonNull final IPublicClientApplication application,
            @NonNull final AcquireTokenParameters parameters,
            @NonNull final OnInteractionRequired interactionRequiredCallback) {
        this.application = application;
        this.parameters = parameters;
        this.interactionRequiredCallback = interactionRequiredCallback;
    }

    public void execute() {
        application.acquireToken(parameters);
        interactionRequiredCallback.handleUserInteraction();
    }
}
