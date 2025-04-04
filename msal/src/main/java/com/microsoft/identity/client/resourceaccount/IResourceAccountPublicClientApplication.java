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
package com.microsoft.identity.client.resourceaccount;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.java.util.TaskCompletedCallbackWithError;

import java.util.List;


/**
 * Expects broker is installed and configured.
 */
public interface IResourceAccountPublicClientApplication extends IPublicClientApplication {

    void getResourceAccountsAsync(final LoadAccountsCallback callback);

    /**
     * Get the list of resource accounts. There can only be one resource account for a device registration.
     * Usually, device is registered in one tenant only so there will be only one resource account returned.
     */
    @WorkerThread
    List<IAccount> getResourceAccounts() throws InterruptedException, MsalException;

    void acquireTokenForResourceAccountAsync(@NonNull final ResoureAccountTokenParameters tokenParameters);

    /**
     * Acquire token for resource account. Account represented by parameters must be resource account associated with device
     * registration into account's home tenant. This method does not show any UI.
     */
    @WorkerThread
    IAuthenticationResult acquireTokenForResourceAccount(@NonNull final ResoureAccountTokenParameters tokenParameters);

    /**
     * Get the AAD device ID for the tenant. A valid device ID is returned only if the device is registered in the provided homeTenantId
     */
    @WorkerThread
    String getAadDeviceId(@NonNull final String homeTenantId);

    void getAadDeviceIdAsync(@NonNull final String homeTenantId, @NonNull final TaskCompletedCallbackWithError<String, MsalException> callback);

    /**
     * Removes the Account and Credentials (tokens) for the supplied IAccount.
     * Should we do device wide remove i.e. remove account altogether from the broker?
     */
    @WorkerThread
    boolean removeAccount(@Nullable final IAccount account) throws MsalException, InterruptedException;

    /**
     * Optional.
     * Acquire token silently for resource account. Account represented by parameters must be resource account associated with device
     * registration into account's home tenant. This method would only only use RT/PRT for acquiring token.
     */
    @WorkerThread
    IAuthenticationResult acquireTokenSilent(@NonNull final AcquireTokenSilentParameters acquireTokenSilentParameters) throws InterruptedException, MsalException;
    void acquireTokenSilentAsync(@NonNull final AcquireTokenSilentParameters acquireTokenSilentParameters);
}
