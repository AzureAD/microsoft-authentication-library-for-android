# Fix for Webview Action Bar Issue (#2341)

## Problem
When using `enableEdgeToEdge()` in an Android app and calling `actionBar?.hide()`, the Microsoft Authentication Library webview ignores the action bar hiding and continues to show it, making content partially unreadable.

## Root Cause
The issue occurs because the MSAL authentication activities (`AuthorizationActivity` and `CurrentTaskAuthorizationActivity`) both extend `DualScreenActivity`, which has logic to handle edge-to-edge mode but does not hide the action bar when edge-to-edge handling is enabled.

## Solution
The fix involves modifying the `DualScreenActivity.initializeContentView()` method to hide the action bar when the `ENABLE_HANDLING_FOR_EDGE_TO_EDGE` flight is enabled.

### Changes Required

In `common/common/src/main/java/com/microsoft/identity/common/internal/ui/DualScreenActivity.java`:

```java
private void initializeContentView(){
    super.setContentView(R.layout.dual_screen_layout);
    if (CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_HANDLING_FOR_EDGE_TO_EDGE)) {
        try {
            // Hide action bar when edge-to-edge is enabled to respect app's action bar visibility
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }
            
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (view, insets) -> {
                int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                int leftInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).left;
                int rightInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).right;

                view.setPadding(leftInset, topInset, rightInset, bottomInset);
                return insets;
            });
        } catch (final Throwable throwable) {
            Logger.warn("DualScreenActivity:initializeContentView", "Failed to set OnApplyWindowInsetsListener");
        }
    }
    adjustLayoutForDualScreenActivity();
}
```

## Key Points

1. **Minimal Change**: Only 3 lines of code added to check and hide the action bar
2. **Feature Flag Protected**: Only activates when `ENABLE_HANDLING_FOR_EDGE_TO_EDGE` flight is enabled
3. **Safe**: Null-check ensures no crashes if no action bar is present
4. **Consistent**: Aligns with edge-to-edge design principles where system UI is hidden

## Testing

Unit tests should be added to verify:
1. Action bar is hidden when edge-to-edge is enabled
2. Action bar is not affected when edge-to-edge is disabled  
3. No crashes occur when no action bar is present

## Impact

This change ensures that MSAL authentication activities respect the parent application's edge-to-edge design choices, providing a consistent user experience.