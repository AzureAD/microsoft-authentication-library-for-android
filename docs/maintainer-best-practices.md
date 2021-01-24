# Best Practices for Contributors & Maintainers
This document is meant to serve a repository of helpful hints, best practices, and how-tos for contributing and maintaining the MSAL and AndroidCommon libraries.

# Table of Contents
1. [Supported Deprecated Lifecycle & Event Callbacks](#deprecatedlifecycle)

---

## Supported Deprecated Lifecycle & Event Callbacks <a name="deprecatedlifecycle"></a>
If a system-invoked callback/lifecycle event has been deprecated and your application supports API-levels both _above and below_ the deprecation version, you must implement the method twice; once for each API level's signature. If possible, provide a common implementation to which the new and old method calls can delegate.

Example: <br/>
`WebViewClient#onReceivedError` ([docs](https://developer.android.com/reference/android/webkit/WebViewClient#onReceivedError(android.webkit.WebView,%2520int,%2520java.lang.String,%2520java.lang.String)))
- Deprecated in API23
- Example app config supports APIs `16` - `29`

Example implementation:
```java
public class MyWebViewClient extends WebViewClient {

    @Override
    @SuppressWarnings("deprecation") // Suppress warnings, if required by your build
    public void onReceivedError(@NonNull final WebView view,
                                final int errorCode,
                                @NonNull final String description,
                                @NonNull final String failingUrl) {
        handleError();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.M) // Annotate the API where available
    public void onReceivedError(@NonNull final WebView view,
                                @NonNull final WebResourceRequest request,
                                @NonNull WebResourceError error) {
        handleError();
    }

    private void handleError() {
        // Perform whatever action, implementation is now common across API levels
    }
}
```