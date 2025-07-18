# Application Creation Guidance
- Cline must use hello-msal-multiple-account and hello-msal-single-account apps in the examples folder as the golden examples when generating an MSAL-integrated application. hello-msal-multiple-account for when the user wants multiple account mode, and hello-msal-single-account for when the user wants single account mode.
- If user does not specify between multiple account mode and single account mode in MSAL, the default mode used should be MULTIPLE. Make sure the configuration json includes which account mode the application is being built for, and use the appropriate example based on this selection.
- Ensure that the latest MSAL Version is used. Reference the MSAL repo to see latest version. At the very least, MSAL 6.+ should be used.
- Cline must copy the following gradle files exactly when creating a new application:
  1. Root level build.gradle (project level) from examples/hello-msal-multiple-account/build.gradle or examples/hello-msal-single-account/build.gradle
  2. App level build.gradle from examples/hello-msal-multiple-account/app/build.gradle or examples/hello-msal-single-account/app/build.gradle
  3. gradle.properties from examples/hello-msal-multiple-account/gradle.properties or examples/hello-msal-single-account/gradle.properties
  4. settings.gradle from examples/hello-msal-multiple-account/settings.gradle or examples/hello-msal-single-account/settings.gradle

  These files must be placed in their corresponding locations in the new application's directory structure. The only modification allowed is updating the applicationId and namespace in app/build.gradle and rootProject.name in settings.gradle to match the new application's package name. Cline can divert from these files as needed once initial creation is complete.

# MSAL API Usage
- ALWAYS use MSAL's Parameters-based APIs instead of deprecated methods. Here are the required patterns:

For interactive token acquisition in multiple account applications:
```java
AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
    .withScopes(SCOPES)
    .withCallback(callback)
    .build();
mPCA.acquireToken(parameters);
```

For silent token acquisition:
```java
AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
    .withScopes(SCOPES)
    .forAccount(account)
    .forceRefresh(false)
    .withCallback(callback)
    .build();
mPCA.acquireTokenSilent(parameters);
```

For sign-in in single account applications only:
```java
SignInParameters parameters = new SignInParameters.Builder()
    .startActivity(activity)
    .withCallback(callback)
    .build();
mPCA.signIn(parameters);
```

Important notes:
- For multiple account applications, use acquireToken for sign in
- For single account applications, use signIn
- acquireTokenWithDeviceCode method is not recommended due to security concerns in the industry. Only use it in niche scenarios where devices lack input methods necessary for interactive authentication. Only add this method to the generated app if explicitly asked for by the user.
- Do not use deprecated methods like:
  - acquireToken(activity, scopes, callback)
  - acquireTokenSilentAsync(scopes, account, authority, callback)
  - signIn(activity, scopes, callback)

# Multiple Account Mode UI Requirements
- Account spinner must include a "No Account Selected" option at position 0
- Sign In button must always be enabled to allow adding new accounts
- Sign Out button must only be enabled when an account is selected in the spinner
- Acquire Token Silent button must only be enabled when an account is selected in the spinner

# Icon and Resource Guidelines
- For application icons, only use resources that are explicitly created in the project
- Do not reference any resources (mipmap/drawable) that haven't been created
- When using adaptive icons:
  1. Create the necessary foreground vector drawable
  2. Define the background color in colors.xml
  3. Create the adaptive icon XML files
  4. Remove any references to non-existent icon resources from AndroidManifest.xml

# Code Implementation Guidelines
- Use ArrayList/List instead of arrays for better API compatibility
- Always initialize member variables in their declaration or constructor
- Use proper access modifiers (private for member variables)
- Follow Android naming conventions (mVariable for member variables)
- Handle UI updates on the main thread using activity.runOnUiThread
- Validate PCA initialization before making any MSAL API calls
- Refresh account lists after authentication operations
- Use proper callback interfaces for communication between components

# Configuration and Manifest
- Use auth_config.template.json as a guide for enabling and disabling MSAL configuration settings based on user needs.
- When generating the configuration file, include all mandatory settings (client_id, redirect_uri, and authorities), and only include optional configurations that differ from their default values.
- Cline must generate an AndroidManifest that includes the user's application name and signature hash. See AndroidManifest.xml in the golden example apps mentioned above for reference. The signature hash and redirect URI in the AndroidManifest.xml must NOT be URL encoded. For example:
  ```xml
  <data
      android:scheme="msauth" 
      android:host="your.app.name"
      android:path="/ABcDeFgJQiLoiEmd-vn14qR*okk=" />
  ```

- When generating the auth_config.json file, the redirect_uri field MUST contain URL encoded values. For example:
  ```json
  {
      "redirect_uri": "msauth://your.app.name/ABcDeFgJQiLoiEmd-vn14qR%2Aokk%3D%0A"
  }
  ```
  Note that special characters like '+' and '=' are URL encoded to %2B and %3D respectively.

- When generating an application, make sure to print the result of the PublicClientApplication creation process so users are aware of any issues. The user can decide to remove this later. Also, no PublicClientApplication APIs should be callable if the PublicClientApplication has not been successfully created.

# Azure App Registration Fields
- If the user prompts cline to create an application and provides the Client ID and redirect uri to be used in the project, Cline must place them in the configuration json and the android manifest.
- If the user does not prompt cline with the client id and redirect uri, then Cline should ask them to enter them as part of a later prompt to be placed in the correct locations mentioned above. Guide them to this link (https://learn.microsoft.com/en-us/entra/identity-platform/msal-client-application-configuration) to assist them in the app registration process.

# UI Requirements and Best Practices

## Theme Configuration
- Applications should use a consistent theme throughout the app
- If using Material Design (optional), extend appropriate Material theme
- Required theme attributes regardless of design system:
  ```xml
  <style name="AppTheme">
      <item name="colorPrimary">@color/colorPrimary</item>
      <item name="colorPrimaryDark">@color/colorPrimaryDark</item>
      <item name="colorAccent">@color/colorAccent</item>
      <item name="android:windowBackground">@color/windowBackground</item>
  </style>
  ```
- Ensure theme is properly set in AndroidManifest.xml using android:theme attribute

## Required Resource Structure
- Mandatory resource directories and files:
  1. res/values/colors.xml - Define standard color palette
  2. res/values/styles.xml - Define app theme and styles
  3. res/values/strings.xml - All string resources
  4. res/layout/ - Layout files
  5. res/drawable/ - Vector drawables and other graphics
  6. res/mipmap-*/ - Launcher icons in various densities
- Icon configuration:
  1. Create adaptive icons using foreground and background layers
  2. Include mipmap-anydpi-v26 for adaptive icon support
  3. Provide round and regular launcher variants
  4. Define ic_launcher_foreground.xml in drawable
  5. Define launcher background color in colors.xml

## UI Component Guidelines
- Use consistent UI components throughout the app
- If using Material Design components:
  1. Include Material Design dependency
  2. Use appropriate Material components
  3. Follow Material Design guidelines
- For standard Android widgets:
  1. Apply consistent styling
  2. Use appropriate view attributes
  3. Implement proper view hierarchies
- Use modern layout systems (ConstraintLayout recommended)
- Implement proper view binding instead of findViewById

## Best Practices
- Resource naming conventions:
  1. Layout files: activity_*, fragment_*, item_*, etc.
  2. Drawable files: ic_*, bg_*, etc.
  3. Color resources: semantic names like colorPrimary, colorError
  4. String resources: screen_action_description pattern
- Layout organization:
  1. Use styles for repeated view attributes
  2. Extract dimensions to dimens.xml
  3. Use proper layout_width/height (wrap_content/match_parent appropriately)
  4. Implement landscape variations where needed
- Theme inheritance:
  1. Create specific styles for different UI components
  2. Use theme overlays when needed
  3. Consider dark theme support
- Error handling:
  1. Show clear error states
  2. Implement proper loading states
  3. Use progress indicators for async operations
