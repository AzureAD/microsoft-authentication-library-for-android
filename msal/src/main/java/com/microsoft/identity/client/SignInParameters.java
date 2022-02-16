package com.microsoft.identity.client;

import android.app.Activity;
import java.util.List;

/**
 * Encapsulates the parameters for calling signIn() in SingleAccountPublicClientApplication.
 */
public class SignInParameters {

    private Activity mActivity;
    private String mLoginHint;
    private List<String> mScopes;
    private Prompt mPrompt;
    private AuthenticationCallback mCallback;

}
