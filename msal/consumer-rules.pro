# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Program Files\Android\android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

##---------------Begin: proguard configuration for MSAL  --------
-keep class com.microsoft.device.display.** { *; }

##---------------Begin: proguard configuration for Moshi  --------
-dontwarn com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
-keep class com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory { *; }

# Keep things as they are used in TypeAdapter for deserialization/serialization
-keep class com.microsoft.identity.client.Logger { *; }
-keep class com.microsoft.identity.client.claims.ClaimsRequest { *; }
-keep class com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation { *; }

##---------------Begin: proguard configuration for Nimbus  ----------
-keep class com.nimbusds.** { *; }

##---------------Begin: proguard configuration for Gson  --------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

# Prevent R8 from leaving Data object members always null
-keepclassmembers class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
