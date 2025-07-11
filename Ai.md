# MSAL Android Integration Guide

## Golden Path Examples
- [Multiple Account Mode Sample](./examples/hello-msal-multiple-account/) - Use this when you want to support multiple accounts in your application (default mode)
- [Single Account Mode Sample](./examples/hello-msal-single-account/) - Use this when you want to support single account authentication only
- [Configuration Template](./auth_config.template.json) -  Configuration template containing explanation of MSAL configuration settings and their default values
- [Code Snippets](./snippets/) - Code snippets showing how to use the MSAL APIs in Java and Kotlin for multiple and single acount modes.

## Required Dependencies
Add these dependencies to your app-level `build.gradle`:

```gradle
android {
    compileSdk 34
    
    defaultConfig {
        minSdk 24
        targetSdk 33
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding true
    }

    packagingOptions {
        resources {
            excludes += ['META-INF/DEPENDENCIES']
        }
    }
}

dependencies {
    def msalVersion = "6.+"
    
    implementation "com.microsoft.identity.client:msal:${msalVersion}"
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    // Optional: Material Design components if using Material theme
    implementation 'com.google.android.material:material:1.9.0'
}
```

## AndroidManifest.xml Requirements

### Required Permissions
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Required Activities
Add these activities to your AndroidManifest.xml inside the `<application>` tag:

```xml
<!-- Main Activity (or your authentication activity) -->
<activity
    android:name=".MainActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<!-- MSAL Browser Tab Activity (Required for authentication) -->
<activity
    android:name="com.microsoft.identity.client.BrowserTabActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        
        <!-- 
        IMPORTANT: 
        - android:host should be your application's package name
        - android:path should be your app's signature hash (NOT URL encoded)
        -->
        <data
            android:scheme="msauth" 
            android:host="your.package.name"
            android:path="/YOUR_SIGNATURE_HASH" />
    </intent-filter>
</activity>
```

## Important Notes
1. The signature hash in AndroidManifest.xml should NOT be URL encoded
2. The redirect URI in auth_config.json MUST be URL encoded
3. The latest MSAL version (6.+ or newer) should be used

For full implementation details, refer to the golden path example apps linked above.
