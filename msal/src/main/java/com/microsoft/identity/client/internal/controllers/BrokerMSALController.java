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

import com.microsoft.identity.common.internal.broker.BrokerRequest;
import com.microsoft.identity.common.internal.broker.IMicrosoftAuthService;
import com.microsoft.identity.common.internal.broker.MicrosoftAuthClient;
import com.microsoft.identity.common.internal.broker.MicrosoftAuthServiceFuture;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class BrokerMSALController extends MSALController {

    @Override
    public AcquireTokenResult acquireToken(MSALAcquireTokenOperationParameters request) {
        Intent interactiveRequestIntent = null;
        IMicrosoftAuthService service = null;

        MicrosoftAuthClient client = new MicrosoftAuthClient(request.getAppContext());
        MicrosoftAuthServiceFuture future = client.connect();

        try {
            service = future.get();
        } catch (Exception e){
            throw new RuntimeException("Exception occurred while awaiting (get) return of MicrosoftAuthService", e);
        }

        try {
            interactiveRequestIntent = service.getIntentForInteractiveRequest();
        } catch (RemoteException e) {
            throw new RuntimeException("Exception occurred while attempting to invoke remote service", e);
        }

        //TODO: We need activity that we can start that will in turn invoke the intent activity
        //startActivity

        return null;
    }

    @Override
    public void completeAcquireToken(int requestCode, int resultCode, Intent data) {




    }

    @Override
    public AcquireTokenResult acquireTokenSilent(MSALAcquireTokenSilentOperationParameters request) {
        Bundle result = null;
        IMicrosoftAuthService service = null;
        Map requestParameters = null;

        MicrosoftAuthClient client = new MicrosoftAuthClient(request.getAppContext());
        MicrosoftAuthServiceFuture future = client.connect();

        try {
            //Do we want a time out here?
            service = future.get();
        } catch (Exception e){
            throw new RuntimeException("Exception occurred while awaiting (get) return of MicrosoftAuthService", e);
        }

        try {
            result = service.acquireTokenSilently(getSilentParameters(request));
        } catch (RemoteException e) {
            throw new RuntimeException("Exception occurred while attempting to invoke remote service", e);
        }

        //Need to map result bundle into AcquireTokenResult
        return null;
    }

    private Map getSilentParameters(MSALAcquireTokenSilentOperationParameters parameters){

        HashMap<String, String> silentParameters = null;

        BrokerRequest request = new BrokerRequest();
        //request.setApplicationName("");
        request.setAuthority(parameters.getAuthority().getAuthorityURL().toString());
        //request.setClaims("");
        request.setClientId(parameters.getClientId());
        request.setCorrelationId(DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID));
        //request.setExtraQueryStringParameter();
        request.setForceRefresh(parameters.getForceRefresh());
        request.setLoginHint(parameters.getAccount().getUsername());
        request.setName(parameters.getAccount().getUsername());
        request.setUserId(parameters.getAccount().getHomeAccountId());
        //request.setPrompt(parameters.get);
        //TODO: This should be the broker redirect URI and not the non-broker redirect URI
        request.setRedirect(parameters.getRedirectUri());
        request.setScope(StringUtil.join(' ', parameters.getScopes()));
        //request.setVersion();


        return silentParameters;
    }

}
