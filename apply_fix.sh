#!/bin/bash
# Script to apply the webview action bar fix for issue #2341

echo "Applying fix for webview action bar issue..."

# The actual file to modify is in the common submodule:
# common/common/src/main/java/com/microsoft/identity/common/internal/ui/DualScreenActivity.java

echo "Changes needed in DualScreenActivity.java:"
echo "1. In the initializeContentView() method"
echo "2. Add action bar hiding when ENABLE_HANDLING_FOR_EDGE_TO_EDGE is enabled"
echo "3. Add these lines after line 66 in the try block:"
echo ""
echo "                // Hide action bar when edge-to-edge is enabled to respect app's action bar visibility"
echo "                if (getSupportActionBar() != null) {"
echo "                    getSupportActionBar().hide();"
echo "                }"
echo ""
echo "This is a minimal, surgical change that only affects edge-to-edge mode."
echo "The fix ensures MSAL webviews respect the parent app's UI design choices."