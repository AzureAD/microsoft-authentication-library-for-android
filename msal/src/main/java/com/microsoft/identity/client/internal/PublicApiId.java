package com.microsoft.identity.client.internal;

public final class PublicApiId {

    // Silent APIs
    public static final String PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS = "21";
    public static final String PCA_ACQUIRE_TOKEN_SILENT_WITH_PARAMETERS = "22";
    public static final String SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_SCOPES_AUTHORITY_CALLBACK = "23";
    public static final String SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_WITH_SCOPES_AUTHORITY = "24";
    public static final String SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS = "25";
    public static final String SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_WITH_PARAMETERS = "26";
    public static final String MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_WITH_SCOPES_ACCOUNT_AUTHORITY = "27";
    public static final String MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_SCOPES_ACCOUNT_AUTHORITY_CALLBACK = "28";

    //Interactive APIs
    public static final String PCA_ACQUIRE_TOKEN_WITH_PARAMETERS = "121";
    public static final String PCA_ACQUIRE_TOKEN_WITH_ACTIVITY_SCOPES_CALLBACK = "122";
    public static final String SINGLE_ACCOUNT_PCA_SIGN_IN = "123";
    public static final String SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_WITH_ACTIVITY_SCOPES_CALLBACK = "124";
    public static final String SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_WITH_PARAMETERS = "125";
    public static final String MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_WITH_ACTIVITY_SCOPES_LOGINHINT_CALLBACK = "126";

    //Get/Remove Accounts APIs
    public static final String SINGLE_ACCOUNT_PCA_GET_CURRENT_ACCOUNT_ASYNC_WITH_CALLBACK= "921";
    public static final String SINGLE_ACCOUNT_PCA_GET_CURRENT_ACCOUNT = "922";
    public static final String SINGLE_ACCOUNT_PCA_SIGN_OUT_WITH_CALLBACK = "923";
    public static final String SINGLE_ACCOUNT_PCA_SIGN_OUT = "924";
    public static final String MULTIPLE_ACCOUNT_PCA_GET_ACCOUNTS_WITH_CALLBACK= "925";
    public static final String MULTIPLE_ACCOUNT_PCA_GET_ACCOUNTS = "926";
    public static final String MULTIPLE_ACCOUNT_PCA_GET_ACCOUNT_WITH_IDENTIFIER_CALLBACK = "927";
    public static final String MULTIPLE_ACCOUNT_PCA_GET_ACCOUNT_WITH_IDENTIFIER = "928";
    public static final String MULTIPLE_ACCOUNT_PCA_REMOVE_ACCOUNT_WITH_ACCOUNT_CALLBACK = "929";
    public static final String MULTIPLE_ACCOUNT_PCA_REMOVE_ACCOUNT_WITH_ACCOUNT = "930";
}
