package com.microsoft.identity.client;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import com.microsoft.identity.client.controllers.RequestCodes;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFuture;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.Future;

public class DeviceBrowserAuthorizationStrategy extends AuthorizationStrategy {

    private AuthorizationResultFuture mAuthorizationResultFuture;

    @Override
    public Future<AuthorizationResult> requestAuthorization(AuthorizationRequest request) throws UnsupportedEncodingException {

        mAuthorizationResultFuture = new AuthorizationResultFuture();

        final Intent intentToStartActivity = new Intent(request.getContext(), AuthenticationActivity.class);
        intentToStartActivity.putExtra(Constants.REQUEST_URL_KEY, request.getAuthorizationRequestAsHttpRequest());

        //TODO: Make this a useful request id if actually required
        intentToStartActivity.putExtra(Constants.REQUEST_ID, 1);

        if (!intentResolved(request.getContext(), intentToStartActivity)) {
            //throw new MsalClientException(MsalClientException.UNRESOLVABLE_INTENT, "The intent is not resolvable");
            throw new RuntimeException("Intent could not be resolved");
        }

        request.getActivity().startActivityForResult(intentToStartActivity, RequestCodes.LOCAL_AUTHORIZATION_REQUEST);

        return mAuthorizationResultFuture;

    }

    @Override
    public void completeAuthorization(int requestCode, int resultCode, Intent data) {

        //Unexpected State until a known state
        //AuthorizationResult result = new AuthorizationResult(AuthorizationStatus.UNEXPECTED_STATE);

        if (requestCode != RequestCodes.LOCAL_AUTHORIZATION_REQUEST) {
            //I think in this case we should just log and ignore.... would be easy for developer to send us something that we don't handle
            return;
        }

        /*
        if (data == null) {
            //Again Log.... set unexpected state
            result = new AuthorizationResult(AuthorizationStatus.UNEXPECTED_STATE);
        }else {

            switch (resultCode) {

                case Constants.UIResponse.CANCEL:
                    result = new AuthorizationResult(AuthorizationStatus.CANCELLED);
                    break;
                case Constants.UIResponse.AUTH_CODE_COMPLETE:
                    AuthorizationResponse response = new AuthorizationResponse();
                    //TODO: Parse response from URL
                    result = new AuthorizationResult(response);
                    break;
                case Constants.UIResponse.AUTH_CODE_ERROR:
                    AuthorizationErrorResponse errorResponse = new AuthorizationErrorResponse();
                    errorResponse.setError(data.getStringExtra(Constants.UIResponse.ERROR_CODE));
                    errorResponse.setErrorDescription(data.getStringExtra(Constants.UIResponse.ERROR_DESCRIPTION));
                    result = new AuthorizationResult(errorResponse);
                    break;
            }
        }
        */

        mAuthorizationResultFuture.setAuthorizationResult(null); //result

    }

    private boolean intentResolved(final Context context, final Intent intent) {
        final ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, 0);
        return resolveInfo != null;
    }

}
