package com.microsoft.identity.client;

public enum WebViewSelection {
    /**
     *
     */
    EMBEDDED_WEBVIEW(1),

    /**
     *
     */
    SYSTEM_BROWSER(2);

    private int mId;

    WebViewSelection(final int id) {
        mId = id;
    }

    public int getId() {
        return mId;
    }
}
