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

package com.microsoft.identity.client;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for {@link AuthorizationResult}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class AuthorizationResultTest {
    static final String REDIRECT_URI = "msauth-clientid://packagename/";
    static final String AUTH_CODE = "authorization_code";
    static final String ERROR_MESSAGE = "access_denied";

    @Test
    public void testResponseUriWithCode() {
        final String responseUri = REDIRECT_URI + "?state=some_state&code=" + AUTH_CODE;
        final AuthorizationResult authorizationResult = AuthorizationResult.parseAuthorizationResponse(responseUri);

        Assert.assertNotNull(authorizationResult);
        Assert.assertTrue(AuthorizationResult.AuthorizationStatus.SUCCESS.equals(authorizationResult.getAuthorizationStatus()));
        Assert.assertTrue(AUTH_CODE.equals(authorizationResult.getAuthCode()));
    }

    @Test
    public void testResponseUriWithError() {
        final String responseUri = REDIRECT_URI + "?" + OauthConstants.Authorize.ERROR + "=" + ERROR_MESSAGE + "&"
                + OauthConstants.Authorize.ERROR_DESCRIPTION + "=" + OauthConstants.Authorize.CANCEL;
        final AuthorizationResult authorizationResult = AuthorizationResult.parseAuthorizationResponse(responseUri);

        Assert.assertNotNull(authorizationResult);
        Assert.assertTrue(AuthorizationResult.AuthorizationStatus.FAIL.equals(
                authorizationResult.getAuthorizationStatus()));
        Assert.assertTrue(ERROR_MESSAGE.equals(authorizationResult.getError()));
        Assert.assertTrue(OauthConstants.Authorize.CANCEL.equalsIgnoreCase(authorizationResult.getErrorDescription()));
    }

    @Test
    public void testResponseUriNotContainCodeOrError() {
        final String responseUri = REDIRECT_URI + "?some_error=access_denied&other_error=some_other_error";
        final AuthorizationResult authorizationResult = AuthorizationResult.parseAuthorizationResponse(responseUri);

        Assert.assertNotNull(authorizationResult);
        Assert.assertTrue(AuthorizationResult.AuthorizationStatus.FAIL.equals(authorizationResult.getAuthorizationStatus()));
        Assert.assertTrue(Constants.MsalInternalError.AUTHORIZATION_FAILED.equals(authorizationResult.getError()));
        Assert.assertTrue(Constants.MsalErrorMessage.AUTHORIZATION_SERVER_INVALID_RESPONSE.equals(authorizationResult.getErrorDescription()));
    }

    @Test
    public void testAuthorizationResultWithUserCancel() {
        final AuthorizationResult authorizationResult = AuthorizationResult.getAuthorizationResultWithUserCancel();
        Assert.assertTrue(AuthorizationResult.AuthorizationStatus.USER_CANCEL.equals(authorizationResult.getAuthorizationStatus()));
        Assert.assertTrue(Constants.MsalInternalError.USER_CANCEL.equals(authorizationResult.getError()));
        Assert.assertTrue(Constants.MsalErrorMessage.USER_CANCELLED_FLOW.equals(authorizationResult.getErrorDescription()));
    }
}
