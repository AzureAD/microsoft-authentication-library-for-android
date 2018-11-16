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
import android.os.RemoteException;

import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.internal.MsalUtils;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.broker.BrokerRequest;
import com.microsoft.identity.common.internal.broker.BrokerResult;
import com.microsoft.identity.common.internal.broker.BrokerResultFuture;
import com.microsoft.identity.common.internal.broker.BrokerTokenResponse;
import com.microsoft.identity.common.internal.broker.IMicrosoftAuthService;
import com.microsoft.identity.common.internal.broker.MicrosoftAuthClient;
import com.microsoft.identity.common.internal.broker.MicrosoftAuthServiceFuture;
import com.microsoft.identity.common.internal.cache.SchemaUtil;
import com.microsoft.identity.common.internal.dto.AccessTokenRecord;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAccount;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.concurrent.ExecutionException;

/**
 * The implementation of MSAL Controller for Broker
 */
public class BrokerMSALController extends MSALController {

    private static final String TAG = BrokerMSALController.class.getSimpleName();

    private BrokerResultFuture mBrokerResultFuture;

    @Override
    public AcquireTokenResult acquireToken(MSALAcquireTokenOperationParameters request) throws ExecutionException, InterruptedException, ClientException {

        //Create BrokerResultFuture to block on response from the broker... response will be return as an activity result
        //BrokerActivity will receive the result and ask the ask the API dispatcher to complete the request
        //In completeAquireToken below we will set the result on the future and unblock the flow
        mBrokerResultFuture = new BrokerResultFuture();

        //Get the broker interactive request intent
        Intent interactiveRequestIntent = getBrokerAuthorizationIntent(request);

        //Pass this intent to the BrokerActivity which will be used to start this activity
        Intent brokerActivityIntent = new Intent(request.getAppContext(), BrokerActivity.class);
        //TODO: Set the request values on the broker intent
        brokerActivityIntent.putExtra(BrokerActivity.BROKER_INTENT, interactiveRequestIntent);

        //Start the BrokerActivity
        request.getActivity().startActivity(brokerActivityIntent);

        //Wait to be notified of the result being returned... we could add a timeout here if we want to
        BrokerResult brokerResult = mBrokerResultFuture.get();

        return null;
    }

    /**
     * Get the intent for the broker interactive request
     *
     * @param request
     * @return
     */
    private Intent getBrokerAuthorizationIntent(MSALAcquireTokenOperationParameters request) throws ClientException {
        Intent interactiveRequestIntent = null;
        IMicrosoftAuthService service = null;

        MicrosoftAuthClient client = new MicrosoftAuthClient(request.getAppContext());
        MicrosoftAuthServiceFuture authServiceFuture = client.connect();

        try {
            service = authServiceFuture.get();
            interactiveRequestIntent = service.getIntentForInteractiveRequest();
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

        //TODO: Map data into broker result and signal future
        mBrokerResultFuture.setBrokerResult(new BrokerResult(new BrokerTokenResponse()));


    }

    @Override
    public AcquireTokenResult acquireTokenSilent(MSALAcquireTokenSilentOperationParameters parameters) throws ClientException {
        IMicrosoftAuthService service = null;

        MicrosoftAuthClient client = new MicrosoftAuthClient(parameters.getAppContext());
        MicrosoftAuthServiceFuture future = client.connect();

        try {
            //Do we want a time out here?
            service = future.get();
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred while awaiting (get) return of MicrosoftAuthService", e);
        }

        try {
            BrokerResult brokerResult = service.acquireTokenSilently(getBrokerRequest(parameters));
            AcquireTokenResult acquireTokenResult = new AcquireTokenResult();
            acquireTokenResult.setTokenResult(brokerResult);
            if (brokerResult.isSuccessful() && brokerResult.getTokenResponse() != null) {
                AuthenticationResult result = getAuthenticationResult(brokerResult.getTokenResponse());
                if (result != null) {
                    acquireTokenResult.setAuthenticationResult(result);
                }
            }
            return acquireTokenResult;
        } catch (RemoteException e) {
            throw new RuntimeException("Exception occurred while attempting to invoke remote service", e);
        }
    }

    /**
     * Map MSALAcquireTokenSilentOperationParameters to Broker Request
     * NOTE: TBD to update this code with BrokerRequest object
     *
     * @param parameters
     * @return {@link BrokerResult}
     */
    private BrokerRequest getBrokerRequest(MSALAcquireTokenSilentOperationParameters parameters) {

        BrokerRequest request = new BrokerRequest();
        request.setApplicationName(parameters.getAppContext().getPackageName());
        request.setAuthority(parameters.getAuthority().getAuthorityURL().toString());
        //request.setClaims("");
        request.setClientId(parameters.getClientId());
        request.setCorrelationId(DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID));
        //request.setExtraQueryStringParameter();
        request.setForceRefresh(parameters.getForceRefresh());
        request.setLoginHint(parameters.getAccount().getUsername());
        request.setName(parameters.getAccount().getUsername());
        request.setUserId(parameters.getAccount().getLocalAccountId());
        //request.setPrompt(parameters.get);
        //TODO: This should be the broker redirect URI and not the non-broker redirect URI
        request.setRedirect(parameters.getRedirectUri());
        //NOTE: Assumption: Broker will handle removing empty string scopes and appending stanard OIDC scopes
        request.setScope(StringUtil.join(' ', parameters.getScopes()));
        //request.setVersion();

        return request;
    }

    private AuthenticationResult getAuthenticationResult(BrokerTokenResponse brokerTokenResponse){
        try {
            ClientInfo clientInfo = new ClientInfo(brokerTokenResponse.getClientInfo());
            String homeAccountId = SchemaUtil.getHomeAccountId(clientInfo);
            String idToken = brokerTokenResponse.getIdToken();
            String tenantId = clientInfo.getUtid();

            AccessTokenRecord accessTokenRecord = new AccessTokenRecord();
            accessTokenRecord.setSecret(brokerTokenResponse.getAccessToken());
            accessTokenRecord.setAuthority(brokerTokenResponse.getTokenType());
            accessTokenRecord.setRealm(tenantId);
            accessTokenRecord.setAuthority(brokerTokenResponse.getAuthority());
            accessTokenRecord.setHomeAccountId(homeAccountId);
            accessTokenRecord.setTarget(brokerTokenResponse.getScope());
            accessTokenRecord.setCredentialType(CredentialType.AccessToken.name());
            accessTokenRecord.setExpiresOn(String.valueOf(MsalUtils.getExpiresOn(brokerTokenResponse.getExpiresIn())));
            accessTokenRecord.setExtendedExpiresOn(String.valueOf(MsalUtils.getExpiresOn(brokerTokenResponse.getExtExpiresIn())));

            MicrosoftStsAccount microsoftStsAccount = new MicrosoftStsAccount(new IDToken(idToken), clientInfo);
            return new AuthenticationResult(accessTokenRecord, idToken, microsoftStsAccount);

        } catch (ServiceException e) {
            Logger.error(TAG, "Unable to construct Authentication result from BrokerTokenResponse ", e);
            return null;
        }

    }

}
