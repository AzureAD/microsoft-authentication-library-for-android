package com.microsoft.identity.client.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.internal.broker.PackageHelper;
import com.microsoft.identity.msal.R;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Activity to show the expected redirect URL (which includes support for Broker)
 */
public class BrokerHelperActivity extends Activity {

    private final String MANIFEST_TEMPLATE =
            "<activity android:name=\"com.microsoft.identity.client.BrowserTabActivity\">\n"
                    + "    <intent-filter>\n"
                    + "        <action android:name=\"android.intent.action.VIEW\" />\n"
                    + "        <category android:name=\"android.intent.category.DEFAULT\" />\n"
                    + "        <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                    + "        <data\n"
                    + "            android:host=\"%s\"\n"
                    + "            android:path=\"/%s\"\n"
                    + "            android:scheme=\"msauth\" />\n"
                    + "    </intent-filter>\n"
                    + "</activity>";
    TextView mPackageName;
    TextView mSignature;
    TextView mRedirect;
    TextView mManifest;

    public static Intent createStartIntent(final Context context) {
        final Intent intent = new Intent(context, BrokerHelperActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.broker_helper);

        mPackageName = findViewById(R.id.txtPackageName);
        mSignature = findViewById(R.id.txtSignature);
        mRedirect = findViewById(R.id.txtRedirect);
        mManifest = findViewById(R.id.txtManifest);

        mSignature.append(getSignature(false));
        mRedirect.append(
                BrokerValidator.getBrokerRedirectUri(
                        this.getApplicationContext(), this.getPackageName()));
        mManifest.append(
                String.format(MANIFEST_TEMPLATE, this.getPackageName(), getSignature(false)));
        mPackageName.append(this.getPackageName());
    }

    private String getSignature(final boolean urlEncode) {

        final PackageHelper info =
                new PackageHelper(this.getApplicationContext().getPackageManager());
        final String signatureDigest = info.getCurrentSignatureForPackage(this.getPackageName());
        String signature = "";

        try {
            if (urlEncode) {
                signature =
                        URLEncoder.encode(signatureDigest, AuthenticationConstants.ENCODING_UTF8);
            } else {
                signature = signatureDigest;
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(
                    "getSignature",
                    "Character encoding "
                            + AuthenticationConstants.ENCODING_UTF8
                            + " is not supported.",
                    e);
            throw new RuntimeException(
                    "Unexpected: Unable to get the signature for this application package.");
        }

        return signature;
    }
}
