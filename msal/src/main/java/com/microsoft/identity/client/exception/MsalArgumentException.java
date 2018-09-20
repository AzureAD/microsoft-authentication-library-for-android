package com.microsoft.identity.client.exception;

public class MsalArgumentException extends MsalException {

    public final static String ACQUIRE_TOKEN_OPERATION_NAME = "acquireToken";
    public final static String ACQUIRE_TOKEN_SILENT_OPERATION_NAME = "acquireToken";

    public final static String SCOPE_ARGUMENT_NAME = "scopes";
    public final static String IACCOUNT_ARGUMENT_NAME = "account";

    private final static String ILLEGAL_ARGUMENT_ERROR_CODE = "illegal_argument_exception";

    private String mOperationName;
    private String mArgumentName;

    public MsalArgumentException(final String operationName, final String argumentName, final String message) {
        super(ILLEGAL_ARGUMENT_ERROR_CODE, message);
        mOperationName = operationName;
        mArgumentName = argumentName;

    }

    public MsalArgumentException(final String operationName, final String argumentName, final String message, final Throwable throwable) {
        super(ILLEGAL_ARGUMENT_ERROR_CODE, message, throwable);
        mOperationName = operationName;
        mArgumentName = argumentName;
    }

    public String getOperationName() {
        return mOperationName;
    }

    public String getArgumentName() {
        return mArgumentName;
    }

}
