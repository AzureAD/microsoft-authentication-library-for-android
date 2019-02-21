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
import android.text.TextUtils;

import com.microsoft.identity.client.internal.MsalUtils;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
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
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.dto.AccessTokenRecord;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAccount;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.result.LocalAuthenticationResult;
import com.microsoft.identity.common.internal.util.QueryParamsAdapter;
import com.microsoft.identity.msal.BuildConfig;

import java.util.concurrent.ExecutionException;

/**
 * The implementation of MSAL Controller for Broker
 */
public class BrokerMSALController extends BaseController {

    private static final String TAG = BrokerMSALController.class.getSimpleName();

    private BrokerResultFuture mBrokerResultFuture;

    @Override
    public AcquireTokenResult acquireToken(AcquireTokenOperationParameters parameters) throws ExecutionException, InterruptedException, ClientException {

        //Create BrokerResultFuture to block on response from the broker... response will be return as an activity result
        //BrokerActivity will receive the result and ask the API dispatcher to complete the request
        //In completeAcquireToken below we will set the result on the future and unblock the flow.
        mBrokerResultFuture = new BrokerResultFuture();

        //Get the broker interactive parameters intent
        Intent interactiveRequestIntent = getBrokerAuthorizationIntent(parameters);
        interactiveRequestIntent.putExtra(AuthenticationConstants.Broker.BROKER_REQUEST_V2, getBrokerRequestForInteractive(parameters));

        //Pass this intent to the BrokerActivity which will be used to start this activity
        Intent brokerActivityIntent = new Intent(parameters.getAppContext(), BrokerActivity.class);
        brokerActivityIntent.putExtra(BrokerActivity.BROKER_INTENT, interactiveRequestIntent);

        //Start the BrokerActivity
        parameters.getActivity().startActivity(brokerActivityIntent);

        //Wait to be notified of the result being returned... we could add a timeout here if we want to
        BrokerResult brokerResult = mBrokerResultFuture.get();

        return getAcquireTokenResult(brokerResult);
    }

    /**
     * Get the intent for the broker interactive request
     *
     * @param request
     * @return
     */
    private Intent getBrokerAuthorizationIntent(AcquireTokenOperationParameters request) throws ClientException {
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
        BrokerResult brokerResult = data.getParcelableExtra(AuthenticationConstants.Broker.BROKER_RESULT_V2);
        mBrokerResultFuture.setBrokerResult(brokerResult);
    }

    @Override
    public AcquireTokenResult acquireTokenSilent(AcquireTokenSilentOperationParameters parameters) throws ClientException {
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
            BrokerResult brokerResult = service.acquireTokenSilently(getBrokerRequestForSilent(parameters));
            return getAcquireTokenResult(brokerResult);
        } catch (RemoteException e) {
            throw new RuntimeException("Exception occurred while attempting to invoke remote service", e);
        }
    }

    /**
     * Map AcquireTokenSilentOperationParameters to Broker Request
     * NOTE: TBD to update this code with BrokerRequest object
     *
     * @param parameters
     * @return {@link BrokerRequest}
     */
    private static BrokerRequest getBrokerRequestForSilent(AcquireTokenSilentOperationParameters parameters) {

        BrokerRequest request = new BrokerRequest();
        request.setApplicationName(parameters.getAppContext().getPackageName());
        request.setAuthority(parameters.getAuthority().getAuthorityURL().toString());
        request.setClientId(parameters.getClientId());
        request.setCorrelationId(DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID));
        request.setForceRefresh(parameters.getForceRefresh());
        request.setUserName(parameters.getAccount().getUsername());
        request.setName(parameters.getAccount().getName());
        request.setHomeAccountId(parameters.getAccount().getHomeAccountId());
        //TODO: This should be the broker redirect URI and not the non-broker redirect URI
        request.setRedirect(parameters.getRedirectUri());
        request.setScope(TextUtils.join(" ", parameters.getScopes()));
        request.setClaims(parameters.getClaimsRequestJson());
        request.setMsalVersion(BuildConfig.VERSION_NAME);

        return request;
    }

    private static BrokerRequest getBrokerRequestForInteractive(AcquireTokenOperationParameters parameters) {
        BrokerRequest request = new BrokerRequest();
        request.setApplicationName(parameters.getAppContext().getPackageName());
        request.setAuthority(parameters.getAuthority().getAuthorityURL().toString());
        request.setClientId(parameters.getClientId());
        request.setCorrelationId(DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID));
        request.setUserName(parameters.getLoginHint());
        request.setName(parameters.getAccount().getName());
        request.setRedirect(parameters.getRedirectUri());
        request.setScope(TextUtils.join(" ", parameters.getScopes()));
        String extraQP = QueryParamsAdapter._toJson(parameters.getExtraQueryStringParameters());
        request.setExtraQueryStringParameter(extraQP);
        request.setClaims(parameters.getClaimsRequestJson());
        return request;
    }

    private static AcquireTokenResult getAcquireTokenResult(BrokerResult brokerResult) {
        AcquireTokenResult acquireTokenResult = new AcquireTokenResult();
        acquireTokenResult.setTokenResult(brokerResult);
        if (brokerResult.isSuccessful() && brokerResult.getTokenResponse() != null) {
            LocalAuthenticationResult result = getAuthenticationResult(brokerResult.getTokenResponse());
            if (result != null) {
                acquireTokenResult.setLocalAuthenticationResult(result);
            }
        }
        return acquireTokenResult;
    }

    private static LocalAuthenticationResult getAuthenticationResult(BrokerTokenResponse brokerTokenResponse) {
        final String methodName = "getLocalAuthenticationResult";
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
            Logger.info(TAG, methodName + " AuthenticationResult successfully returned ");
            return new LocalAuthenticationResult(
                    accessTokenRecord,
                    brokerTokenResponse.getRefreshToken(),
                    idToken,
                    microsoftStsAccount
            );

        } catch (ServiceException e) {
            Logger.error(TAG, "Unable to construct Authentication result from BrokerTokenResponse ", e);
            return null;
        }

    }

}
