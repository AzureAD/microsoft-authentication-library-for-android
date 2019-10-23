package com.microsoft.testing.popbenchmarker;

import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.impl.RSAKeyUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.microsoft.testing.popbenchmarker.PopUtils2.ANDROID_KEYSTORE;
import static com.microsoft.testing.popbenchmarker.PopUtils2.KEYSTORE_ALIAS;
import static com.microsoft.testing.popbenchmarker.PopUtils2.getInitializedRsaKeyPairGenerator;
import static com.microsoft.testing.popbenchmarker.PopUtils2.isInsideSecureHardware;


public class MainActivity extends AppCompatActivity {

    private MainThreadMarshaller<String> mThreadMarshaller = new MainThreadMarshaller<>();

    private List<Long> mKeyGenerationTimings = new ArrayList<>();
    private List<Long> mKeyLoadTimings = new ArrayList<>();
    private List<Long> mSigningTimings = new ArrayList<>();

    private TextView
            mTv_Manufacturer,
            mTv_Model,
            mTv_OsVer,
            mTv_ApiLevel,
            mTv_KeyGen,
            mTv_KeyLoad,
            mTv_Signing,
            mTv_HardwareIsolated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeViews();
    }

    @Override
    protected void onResume() {
        super.onResume();

        int iterations = 100;

        while (iterations > 1) {
            populateViews();
            iterations--;
        }

        computeAverages();
    }

    private void computeAverages() {
        final long avgKeyGen = computeAvg(mKeyGenerationTimings);
        final long avgKeyLoad = computeAvg(mKeyLoadTimings);
        final long avgSign = computeAvg(mSigningTimings);
        setText(mTv_KeyGen, String.valueOf(avgKeyGen));
        setText(mTv_KeyLoad, String.valueOf(avgKeyLoad));
        setText(mTv_Signing, String.valueOf(avgSign));
    }

    private long computeAvg(List<Long> list) {
        long sum = 0L;

        for (final Long timing : list) {
            sum += timing;
        }

        return sum / list.size();
    }

    private void initializeViews() {
        mTv_Manufacturer = findViewById(R.id.disp_manf);
        mTv_Model = findViewById(R.id.disp_model);
        mTv_OsVer = findViewById(R.id.disp_osver);
        mTv_ApiLevel = findViewById(R.id.disp_api_lvl);
        mTv_KeyGen = findViewById(R.id.disp_key_gen);
        mTv_KeyLoad = findViewById(R.id.disp_key_load);
        mTv_Signing = findViewById(R.id.disp_signing);
        mTv_HardwareIsolated = findViewById(R.id.disp_hardware_iso);
    }

    private void populateViews() {
        setText(mTv_Manufacturer, Build.MANUFACTURER);
        setText(mTv_Model, Build.MODEL);
        setText(mTv_OsVer, Build.VERSION.RELEASE);
        setText(mTv_ApiLevel, Build.VERSION.SDK_INT);

        final LinkedList<Runnable> tasks = new LinkedList<>();
        tasks.add(new Runnable() {
            @Override
            public void run() {
                getKeyLoadTiming(new AsyncResultCallback<String>() {
                    @Override
                    public void onDone(String result) {
                        setText(mTv_KeyLoad, result);
                        tasks.remove().run();
                    }
                });
            }
        });

        tasks.add(new Runnable() {
            @Override
            public void run() {
                getSigningTiming(new AsyncResultCallback<String>() {
                    @Override
                    public void onDone(String result) {
                        setText(mTv_Signing, result);
                        tasks.remove().run();
                    }
                });
            }
        });

        tasks.add(new Runnable() {
            @Override
            public void run() {
                getIsHardwareIsolated(new AsyncResultCallback<String>() {
                    @Override
                    public void onDone(String result) {
                        setText(mTv_HardwareIsolated, result);
                        // Done!
                    }
                });
            }
        });

        getKeyGenerationTiming(new AsyncResultCallback<String>() {
            @Override
            public void onDone(String result) {
                setText(mTv_KeyGen, result);
                tasks.remove().run();
            }
        });
    }

    private void getIsHardwareIsolated(@NonNull final AsyncResultCallback<String> callback) {
        final Timer.TimerResult<Boolean> result = Timer.execute(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
                keyStore.load(null);
                final KeyStore.Entry entry = keyStore.getEntry(KEYSTORE_ALIAS, null);
                PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
                return isInsideSecureHardware(privateKey);
            }
        });
        mThreadMarshaller.postResult(result.mResult.toString(), callback);
    }

    private void getSigningTiming(@NonNull final AsyncResultCallback<String> callback) {
        try {
            // Prepare JWT with claims set
            final JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject("test")
                    .issuer("https://login.microsoftonline.com")
                    .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                    .build();

            final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);

            final KeyStore.Entry entry = keyStore.getEntry(KEYSTORE_ALIAS, null);
            final PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
            //final PublicKey publicKey = keyStore.getCertificate(KEYSTORE_ALIAS).getPublicKey();
            //final KeyPair restoredKeyPair = new KeyPair(publicKey, privateKey);
            //final RSAKey key = getRsaKey(restoredKeyPair);

            final RSASSASigner signer = new RSASSASigner(privateKey);
            //signer.getJCAContext().setProvider(keyStore.getProvider());
            //
            final SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            //.keyID(key.getKeyID())
                            .build(),
                    claimsSet
            );

            final Timer.TimerResult<Void> result = Timer.execute(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    // Compute the RSA signature
                    signedJWT.sign(signer);

                    // To serialize to compact form, produces something like
                    // eyJhbGciOiJSUzI1NiJ9.SW4gUlNBIHdlIHRydXN0IQ.IRMQENi4nJyp4er2L
                    // mZq3ivwoAjqa1uUkSBKFIX7ATndFF5ivnt-m8uApHO4kfIFOrW7w2Ezmlg3Qd
                    // maXlS9DhN0nUk_hGI3amEjkKd0BWYCB8vfUbUv0XGjQip78AI4z1PrFRNidm7
                    // -jPDm5Iq0SZnjKjCNS5Q15fokXZc8u0A
                    final String signed = signedJWT.serialize();
                    //
                    return null;
                }
            });
            mSigningTimings.add(result.mDuration);
            mThreadMarshaller.postResult(String.valueOf(result.mDuration), callback);
        } catch (Exception e) {
            crash(e);
        }
    }

    private void getKeyLoadTiming(@NonNull final AsyncResultCallback<String> callback) {
        final Timer.TimerResult<Void> result = Timer.execute(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
                keyStore.load(null);
                keyStore.getEntry(KEYSTORE_ALIAS, null);
                return null;
            }
        });
        mKeyLoadTimings.add(result.mDuration);
        mThreadMarshaller.postResult(String.valueOf(result.mDuration), callback);
    }

    private void getKeyGenerationTiming(@NonNull final AsyncResultCallback<String> callback) {
        final Timer.TimerResult<Void> result = Timer.execute(new Callable<Void>() {
            @Override
            public Void call() {
                KeyPairGenerator kpg;

                try {
                    while (true) {
                        kpg = getInitializedRsaKeyPairGenerator(MainActivity.this);
                        final KeyPair kp = kpg.generateKeyPair();
                        // Check that the generated thumbprint size is 2048, if it is not, retry...
                        final int length = RSAKeyUtils.keyBitLength(kp.getPrivate());

                        if (length >= 2048) {
                            break;
                        }
                    }

                } catch (UnsupportedOperationException e) {
                    crash(e);
                }

                return null;
            }
        });
        mKeyGenerationTimings.add(result.mDuration);
        mThreadMarshaller.postResult(String.valueOf(result.mDuration), callback);
    }

    private void crash(final Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
    }

    private void setText(@NonNull final TextView textView,
                         @NonNull final Object result) {
        textView.setText(result.toString());
    }

    interface AsyncResultCallback<T> {
        void onDone(T result);
    }

    class MainThreadMarshaller<T> {
        void postResult(final T result,
                        final AsyncResultCallback<T> callback) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    callback.onDone(result);
                }
            });
        }
    }
}
