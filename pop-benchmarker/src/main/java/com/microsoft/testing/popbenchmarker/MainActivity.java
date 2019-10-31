//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.testing.popbenchmarker;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;


public class MainActivity extends AppCompatActivity {

    private MainThreadMarshaller<String> mThreadMarshaller = new MainThreadMarshaller<>();

    private List<Long> mKeyGenerationTimings = new ArrayList<>();
    private List<Long> mKeyLoadTimings = new ArrayList<>();
    private List<Long> mSigningTimings = new ArrayList<>();

    private TextView
            mTvManufacturer,
            mTvModel,
            mTvOsVer,
            mTvApiLevel,
            mTvKeyGen,
            mTvKeyLoad,
            mTvSigning,
            mTvHardwareIsolated;

    private Button mBtn_Restart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeViews();
        setText(mTvManufacturer, Build.MANUFACTURER);
        setText(mTvModel, Build.MODEL);
        setText(mTvOsVer, Build.VERSION.RELEASE);
        setText(mTvApiLevel, Build.VERSION.SDK_INT);

        executeBenchmarks();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void executeBenchmarks() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int iterations = 20;
                int thisIteration = iterations;

                while (thisIteration >= 1) {
                    final int finalThisIteration = thisIteration;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mBtn_Restart.setText("Remaining iterations: " + finalThisIteration + " of: " + iterations);
                        }
                    });
                    populateViews();
                    thisIteration--;
                }

                computeAverages();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBtn_Restart.setText("Restart");
                        mBtn_Restart.setEnabled(true);
                    }
                });
            }
        }).start();
    }

    private void computeAverages() {
        final long avgKeyGen = computeAvg(mKeyGenerationTimings);
        final long avgKeyLoad = computeAvg(mKeyLoadTimings);
        final long avgSign = computeAvg(mSigningTimings);
        setText(mTvKeyGen, String.valueOf(avgKeyGen));
        setText(mTvKeyLoad, String.valueOf(avgKeyLoad));
        setText(mTvSigning, String.valueOf(avgSign));
    }

    private long computeAvg(List<Long> list) {
        long sum = 0L;

        for (final Long value : list) {
            sum += value;
        }

        return sum / list.size();
    }

    private void initializeViews() {
        mTvManufacturer = findViewById(R.id.disp_manf);
        mTvModel = findViewById(R.id.disp_model);
        mTvOsVer = findViewById(R.id.disp_osver);
        mTvApiLevel = findViewById(R.id.disp_api_lvl);
        mTvKeyGen = findViewById(R.id.disp_key_gen);
        mTvKeyLoad = findViewById(R.id.disp_key_load);
        mTvSigning = findViewById(R.id.disp_signing);
        mTvHardwareIsolated = findViewById(R.id.disp_hardware_iso);
        mBtn_Restart = findViewById(R.id.btn_restart);
        mBtn_Restart.setEnabled(false);
        mBtn_Restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.isEnabled()) {
                    v.setEnabled(false);

                    restartBenchmarks();
                }
            }
        });
    }

    private void restartBenchmarks() {
        mKeyGenerationTimings.clear();
        mKeyLoadTimings.clear();
        mSigningTimings.clear();

        final String calculating = "Calculating...";

        setText(mTvKeyGen, calculating);
        setText(mTvKeyLoad, calculating);
        setText(mTvSigning, calculating);

        executeBenchmarks();
    }

    private static final Semaphore sLOCK = new Semaphore(1);

    private void populateViews() {
        try {
            sLOCK.acquire();
        } catch (InterruptedException e) {
            crash(e);
        }
        final LinkedList<Runnable> tasks = new LinkedList<>();
        tasks.add(new Runnable() {
            @Override
            public void run() {
                getKeyLoadTiming(new AsyncResultCallback<String>() {
                    @Override
                    public void onDone(String result) {
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
                        setText(mTvHardwareIsolated, result);
                        // Done!
                        sLOCK.release();
                    }
                });
            }
        });

        getKeyGenerationTiming(new AsyncResultCallback<String>() {
            @Override
            public void onDone(String result) {
                tasks.remove().run();
            }
        });
    }

    private void getIsHardwareIsolated(@NonNull final AsyncResultCallback<String> callback) {
        final Timer.TimerResult<Boolean> result = Timer.execute(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return false;
            }
        });
        mThreadMarshaller.postResult(result.mResult.toString(), callback);
    }

    private void getSigningTiming(@NonNull final AsyncResultCallback<String> callback) {
        RSAKey key = null;
        JWSSigner signer = null;
        try {
            key = RSAKey.parse(
                    NimbusPoPUtils.readSecurePref(
                            MainActivity.this,
                            "pop-rsakey"
                    )
            );
            signer = NimbusPoPUtils.getSigner(key);
        } catch (ParseException | JOSEException e) {
            e.printStackTrace();
        }
        // Prepare JWT with claims set
        final JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("test")
                .issuer("https://login.microsoftonline.com")
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();
        try {
            final RSAKey finalKey = key;
            final JWSSigner finalSigner = signer;
            final Timer.TimerResult<Void> result = Timer.execute(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    final SignedJWT signedJWT = NimbusPoPUtils.getSignedJwt(
                            finalSigner,
                            claimsSet,
                            UUID.randomUUID().toString()
                    );
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
                final RSAKey key = RSAKey.parse(
                        NimbusPoPUtils.readSecurePref(
                                MainActivity.this,
                                "pop-rsakey"
                        )
                );
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
                try {
                    final RSAKey rsaKey = NimbusPoPUtils.generateRsaKeyPair(
                            NimbusPoPUtils.MINIMUM_RSA_COMPLEXITY,
                            null,
                            null, // TODO can I set the AndroidKeyStore here?
                            null,
                            null
                    );

                    // Write the values for later....
                    NimbusPoPUtils.writeSecurePref(
                            MainActivity.this,
                            "pop-rsakey",
                            rsaKey.toJSONString()
                    );

                } catch (JOSEException e) {
                    e.printStackTrace();
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(result.toString());
            }
        });
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
