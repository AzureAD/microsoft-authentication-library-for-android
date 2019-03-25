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
package com.microsoft.identity.client.internal.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;

import com.microsoft.identity.client.IMicrosoftAuthService;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.broker.BrokerResultFuture;
import com.microsoft.identity.common.internal.broker.MicrosoftAuthClient;
import com.microsoft.identity.common.internal.broker.MicrosoftAuthServiceFuture;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.MsalBrokerRequestAdapter;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.result.MsalBrokerResultAdapter;

/**
 * The implementation of MSAL Controller for Broker
 */
public class BrokerMsalController extends BaseController {

    private static final String TAG = BrokerMsalController.class.getSimpleName();

    private BrokerResultFuture mBrokerResultFuture;

    @Override
    public AcquireTokenResult acquireToken(AcquireTokenOperationParameters parameters)
            throws  InterruptedException, BaseException {

        //Create BrokerResultFuture to block on response from the broker... response will be return as an activity result
        //BrokerActivity will receive the result and ask the API dispatcher to complete the request
        //In completeAcquireToken below we will set the result on the future and unblock the flow.
        mBrokerResultFuture = new BrokerResultFuture();

        //Get the broker interactive parameters intent
        final Intent interactiveRequestIntent = getBrokerAuthorizationIntent(parameters);

        final MsalBrokerRequestAdapter msalBrokerRequestAdapter = new MsalBrokerRequestAdapter();
        interactiveRequestIntent.putExtra(
                AuthenticationConstants.Broker.BROKER_REQUEST_V2,
                msalBrokerRequestAdapter.bundleFromAcquireTokenParameters(parameters)
        );

        //Pass this intent to the BrokerActivity which will be used to start this activity
        final Intent brokerActivityIntent = new Intent(parameters.getAppContext(), BrokerActivity.class);
        brokerActivityIntent.putExtra(BrokerActivity.BROKER_INTENT, interactiveRequestIntent);

        //Start the BrokerActivity
        parameters.getActivity().startActivity(brokerActivityIntent);

        //Wait to be notified of the result being returned... we could add a timeout here if we want to
        final Bundle resultBundle = mBrokerResultFuture.get();

        return getAcquireTokenResult(resultBundle);

    }

    /**
     * Get the intent for the broker interactive request
     *
     * @param request
     * @return
     */
    private Intent getBrokerAuthorizationIntent(@NonNull final AcquireTokenOperationParameters request) throws ClientException {
        Intent interactiveRequestIntent;
        IMicrosoftAuthService service;

        final MicrosoftAuthClient client = new MicrosoftAuthClient(request.getAppContext());
        final MicrosoftAuthServiceFuture authServiceFuture = client.connect();

        try {
            service = authServiceFuture.get();
            interactiveRequestIntent = service.getIntentForInteractiveRequest();

            // TODO : See what's the right exception here
        } catch (RemoteException e) {
            throw new RuntimeException("Exception occurred while attempting to invoke remote service", e);
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred while awaiting (get) return of MicrosoftAuthService", e);
        } finally {
            client.disconnect();
        }

        return interactiveRequestIntent;
    }

    /**
     * Get the response from the Broker captured by BrokerActivity.
     * BrokerActivity will pass along the response to the broker controller
     * The Broker controller will map th response into the broker result
     * And signal the future with the broker result to unblock the request.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void completeAcquireToken(int requestCode, int resultCode, Intent data) {
        final Bundle resultBundle = data.getBundleExtra(AuthenticationConstants.Broker.BROKER_RESULT_V2);
        mBrokerResultFuture.setResultBundle(resultBundle);
    }

    @Override
    public AcquireTokenResult acquireTokenSilent(AcquireTokenSilentOperationParameters parameters) throws BaseException {
        IMicrosoftAuthService service;

        MicrosoftAuthClient client = new MicrosoftAuthClient(parameters.getAppContext());
        MicrosoftAuthServiceFuture future = client.connect();

        try {
            //Do we want a time out here?
            service = future.get();
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred while awaiting (get) return of MicrosoftAuthService", e);
        }

        try {
            final MsalBrokerRequestAdapter msalBrokerRequestAdapter = new MsalBrokerRequestAdapter();
            final Bundle resultBundle = service.acquireTokenSilently(
                    msalBrokerRequestAdapter.bundleFromSilentOperationParameters(parameters)
            );
            return getAcquireTokenResult(resultBundle);

        } catch (RemoteException e) {
            // TODO : See what's the right exception here
            throw new RuntimeException("Exception occurred while attempting to invoke remote service", e);
        }
    }

    private AcquireTokenResult getAcquireTokenResult(final Bundle resultBundle) throws BaseException {

        final MsalBrokerResultAdapter resultAdapter = new MsalBrokerResultAdapter();

        if(resultBundle.getBoolean(AuthenticationConstants.Broker.BROKER_REQUEST_V2_SUCCESS)){
            Logger.verbose(TAG, "Successful result from the broker ");

            final AcquireTokenResult acquireTokenResult = new AcquireTokenResult();
            acquireTokenResult.setLocalAuthenticationResult(
                    resultAdapter.authenticationResultFromBundle(resultBundle)
            );

            return acquireTokenResult;
        }

        Logger.warn(TAG, "Exception returned from broker, retrieving exception details ");

        throw resultAdapter.baseExceptionFromBundle(resultBundle);
    }

}
