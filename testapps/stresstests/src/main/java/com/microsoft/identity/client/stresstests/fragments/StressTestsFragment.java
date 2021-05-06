package com.microsoft.identity.client.stresstests.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.stresstests.INotifyOperationResultCallback;
import com.microsoft.identity.client.stresstests.AsyncResult;
import com.microsoft.identity.client.stresstests.R;
import com.microsoft.identity.client.stresstests.Util;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.internal.result.ResultFuture;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.SneakyThrows;

public abstract class StressTestsFragment<T, S> extends Fragment {

    private static final String TAG = StressTestsFragment.class.getSimpleName();

    private IPublicClientApplication mApp;
    private TextView
            titleTextView,
            timeRemainingTextView,
            timeElapsedTextView,
            numThreadsTextView,
            resultsTextView,
            errorTextView,
            timeLimitTextView;
    private Button startButton, stopButton, retryButton;
    private View progressView, mainContent, layoutError;
    private ProgressBar loadingContent;
    private ScrollView resultsScrollView;

    private ExecutorService executorService;
    private Thread executorThread;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_base, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        titleTextView = view.findViewById(R.id.testsTitle);
        timeRemainingTextView = view.findViewById(R.id.timeRemainingTextView);
        timeElapsedTextView = view.findViewById(R.id.timeElapsedTextView);
        numThreadsTextView = view.findViewById(R.id.numThreadsTextView);
        resultsTextView = view.findViewById(R.id.resultsTextView);
        errorTextView = view.findViewById(R.id.errorTextView);
        timeLimitTextView = view.findViewById(R.id.timeLimitTextView);

        startButton = view.findViewById(R.id.startTestsButton);
        stopButton = view.findViewById(R.id.stopTestsButton);
        retryButton = view.findViewById(R.id.retryButton);

        progressView = view.findViewById(R.id.layoutProgress);
        mainContent = view.findViewById(R.id.mainContent);

        loadingContent = view.findViewById(R.id.loadingContent);
        layoutError = view.findViewById(R.id.layoutError);

        resultsScrollView = view.findViewById(R.id.resultScrollView);


        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initialize();
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executorThread = run();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (executorService != null) {
                    executorService.shutdown();
                    executorThread.interrupt();
                    executorService = null;
                }
                stopExecution();
            }
        });

        initialize();
    }

    private AsyncResult<S> runAsync(T prerequisites) throws ExecutionException, InterruptedException {
        final ResultFuture<AsyncResult<S>> resultFuture = new ResultFuture<>();

        this.run(prerequisites, mApp, new INotifyOperationResultCallback<S>() {
            @Override
            public void onSuccess(S result) {
                resultFuture.setResult(new AsyncResult<S>(result, true));
            }

            @Override
            public void onError(String message) {
                resultFuture.setResult(new AsyncResult<S>(null, false));
                printOutput(message);
            }
        });

        return resultFuture.get();
    }


    private AsyncResult<T> prepareAsync() throws ExecutionException, InterruptedException {
        final ResultFuture<AsyncResult<T>> resultFuture = new ResultFuture<>();

        this.prepare(mApp, new INotifyOperationResultCallback<T>() {
            @Override
            public void onSuccess(T result) {
                resultFuture.setResult(new AsyncResult<>(result, true));
            }

            @Override
            public void onError(String message) {
                resultFuture.setResult(new AsyncResult<T>(null, false));
                printOutput(message);
            }
        });

        return resultFuture.get();
    }


    /**
     * Runs the stress tests
     */
    private Thread run() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mainContent.post(new Runnable() {
                        @Override
                        public void run() {
                            startButton.setEnabled(false);
                            progressView.setVisibility(View.VISIBLE);
                            stopButton.setEnabled(true);
                        }
                    });

                    final AsyncResult<T> response = prepareAsync();

                    if (response.isSuccess()) {
                        final BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(getNumberOfThreads());
                        final RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
                        executorService = new ThreadPoolExecutor(1, getNumberOfThreads(), 0L, TimeUnit.MILLISECONDS, blockingQueue, rejectedExecutionHandler);

                        final long startTime = System.currentTimeMillis();
                        final long timeLimit = getTimeLimit() * 60 * 1000;

                        while (System.currentTimeMillis() - startTime < timeLimit) {
                            executorService.submit(new Runnable() {
                                @SneakyThrows
                                @Override
                                public void run() {
                                    runAsync(response.getResult());
                                }
                            });
                            Thread.sleep(50);
                        }
                        executorService.shutdown();
                        stopExecution();

                        printOutput("All tests done!");
                    } else {
                        stopExecution();
                    }

                } catch (InterruptedException | ExecutionException exception) {
                    printOutput("An error occurred while running tests: " + exception.getMessage());
                }
            }
        });


        thread.start();

        return thread;
    }

    private void stopExecution() {
        mainContent.post(new Runnable() {
            @Override
            public void run() {
                startButton.setEnabled(true);
                progressView.setVisibility(View.GONE);
                stopButton.setEnabled(false);
            }
        });
    }

    private void initialize() {
        loadingContent.setVisibility(View.VISIBLE);
        mainContent.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);

        titleTextView.setText(getTitle());

        this.createApplication(new INotifyOperationResultCallback<IPublicClientApplication>() {
            @Override
            public void onSuccess(IPublicClientApplication result) {
                mApp = result;
                layoutError.setVisibility(View.GONE);
                loadingContent.setVisibility(View.GONE);
                mainContent.setVisibility(View.VISIBLE);


                progressView.setVisibility(View.GONE);
                stopButton.setEnabled(false);

                timeLimitTextView.setText(Util.timeString(getTimeLimit() * 60));
                timeElapsedTextView.setText("--");
                timeRemainingTextView.setText("--");
                numThreadsTextView.setText(String.valueOf(getNumberOfThreads()));
            }

            @Override
            public void onError(String message) {
                layoutError.setVisibility(View.VISIBLE);
                loadingContent.setVisibility(View.GONE);
                mainContent.setVisibility(View.GONE);

                errorTextView.setText(message);
            }
        });

    }


    private void createApplication(final INotifyOperationResultCallback<IPublicClientApplication> callback) {
        // Provide secret key for token encryption
        final String methodName = ":createApplication";
        try {
            if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
                SecretKeyFactory keyFactory = SecretKeyFactory
                        .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
                SecretKey tempKey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
                        "abcdedfdfd".getBytes("UTF-8"), 100, 256));
                SecretKey secretKey = new SecretKeySpec(tempKey.getEncoded(), "AES");
                AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | UnsupportedEncodingException exception) {
            Log.e(TAG + methodName, "Failed to generate secret key", exception);
            callback.onError("Failed to generate secret key: " + exception.getMessage());
        }

        PublicClientApplication.create(getContext(), R.raw.msal_config_default, new IPublicClientApplication.ApplicationCreatedListener() {
            @Override
            public void onCreated(IPublicClientApplication application) {
                callback.onSuccess(application);
            }

            @Override
            public void onError(MsalException exception) {
                Log.e(TAG + methodName, "Failed to load MSAL Application", exception);
                callback.onError(String.format("Failed to load MSAL Application: %s", exception.getMessage()));
            }
        });
    }

    public synchronized void printOutput(final String text) {
        final String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                resultsTextView.append(String.format("%s: %s\n", currentTime, text));
                resultsScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }


    /**
     * Provide the title that will be displayed at the top of the layout
     *
     * @return the title of the tests running
     */
    public abstract String getTitle();

    /**
     * Provide the number of threads that will be running
     *
     * @return the number of threads
     */
    public abstract int getNumberOfThreads();

    /**
     * Provide the time limit for the tests in minutes.
     *
     * @return the time limit in minutes
     */
    public abstract int getTimeLimit();

    /**
     * Prepare the fragment for running the tests. The callback's result will be passed to every execution
     *
     * @param application the MSAL IPublicClientApplication
     * @param callback    the callback to set the success/failure during preparation
     */
    public abstract void prepare(IPublicClientApplication application, INotifyOperationResultCallback<T> callback);


    /**
     * Run the execution via a thread pool.
     *
     * @param prerequisites the result received from the {@link StressTestsFragment#prepare(IPublicClientApplication, INotifyOperationResultCallback)} method
     * @param application   the MSAL IPublicClientApplication
     * @param callback      the callback to show whether the execution was successful or failed.
     */
    public abstract void run(T prerequisites, IPublicClientApplication application, INotifyOperationResultCallback<S> callback);
}
