package com.microsoft.identity.client;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.support.annotation.NonNull;

import com.microsoft.identity.client.controllers.RequestCodes;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftSts;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFuture;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.Future;

public class DeviceBrowserAuthorizationStrategy extends AuthorizationStrategy {

    private AuthorizationResultFuture mAuthorizationResultFuture;
    private Activity mActivity;
    private OAuth2Strategy mOAuth2Strategy;

    public DeviceBrowserAuthorizationStrategy(@NonNull OAuth2Strategy strategy, @NonNull Activity activity){
        mOAuth2Strategy = strategy;
        mActivity = activity;
    }

    @Override
    public Future<AuthorizationResult> requestAuthorization(AuthorizationRequest request) throws UnsupportedEncodingException {

        mAuthorizationResultFuture = new AuthorizationResultFuture();

        final Intent intentToStartActivity = new Intent(mActivity.getApplicationContext(), AuthenticationActivity.class);
        intentToStartActivity.putExtra(Constants.REQUEST_URL_KEY, request.getAuthorizationRequestAsHttpRequest());

        //TODO: Make this a useful request id if actually required
        intentToStartActivity.putExtra(Constants.REQUEST_ID, 1);

        if (!intentResolved(mActivity.getApplicationContext(), intentToStartActivity)) {
            //throw new MsalClientException(MsalClientException.UNRESOLVABLE_INTENT, "The intent is not resolvable");
            throw new RuntimeException("Intent could not be resolved");
        }

        mActivity.startActivityForResult(intentToStartActivity, RequestCodes.LOCAL_AUTHORIZATION_REQUEST);

        return mAuthorizationResultFuture;

    }

    @Override
    public void completeAuthorization(int requestCode, int resultCode, Intent data) {

        if (requestCode != RequestCodes.LOCAL_AUTHORIZATION_REQUEST) {
            //I think in this case we should just log and ignore.... would be easy for developer to send us something that we don't handle
            return;
        }


        AuthorizationResultFactory factory = mOAuth2Strategy.getAuthorizationResultFactory();
        AuthorizationResult result = factory.createAuthorizationResult(resultCode, data);

        mAuthorizationResultFuture.setAuthorizationResult(result); //result

    }

    private boolean intentResolved(final Context context, final Intent intent) {
        final ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, 0);
        return resolveInfo != null;
    }

    public Activity getActivity() {
        return mActivity;
    }

    public void setActivity(Activity activity) {
        this.mActivity = activity;
    }
}
