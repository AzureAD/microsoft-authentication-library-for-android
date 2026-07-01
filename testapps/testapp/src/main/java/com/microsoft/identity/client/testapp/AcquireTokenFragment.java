//   Copyright (c) Microsoft Corporation.
//   All rights reserved.
//
//   This code is licensed under the MIT License.
//
//   Permission is hereby granted, free of charge, to any person obtaining a copy
//   of this software and associated documentation files(the "Software"), to deal
//   in the Software without restriction, including without limitation the rights
//   to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//   copies of the Software, and to permit persons to whom the Software is
//   furnished to do so, subject to the following conditions :
//
//   The above copyright notice and this permission notice shall be included in
//   all copies or substantial portions of the Software.
//
//   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//   THE SOFTWARE.
package com.microsoft.identity.client.testapp;

import static com.microsoft.identity.client.testapp.R.id.enablePII;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.fragment.app.Fragment;

import com.microsoft.identity.client.HttpMethod;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.Logger;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.common.internal.providers.EncryptedBrokerInstallResumeStore;
import com.microsoft.identity.common.java.providers.BrokerInstallResumeRequest;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.internal.activebrokerdiscovery.BrokerDiscoveryClientFactory;
import com.microsoft.identity.common.internal.broker.BrokerData;
import com.microsoft.identity.common.internal.cache.ClientActiveBrokerCache;
import com.microsoft.identity.common.internal.cache.IClientActiveBrokerCache;
import com.microsoft.identity.common.java.opentelemetry.OTelUtility;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;

/**
 * acquireToken Fragment, contains the flow for acquireToken interactively, acquireTokenSilent, getUsers, removeUser.
 */
public class AcquireTokenFragment extends Fragment {
    public static final String NONE_NULL = "NONE (NULL)";

    /**
     * Single-use correlation id of a broker-install resume (POC). When present, the fragment reads
     * the FULL persisted request from the encrypted store keyed by this id and resumes the original
     * interactive request from that snapshot — it does NOT reconstruct the request from UI fields.
     */
    public static final String ARG_RESUME_CORRELATION_ID = "resume_correlation_id";

    /** True until the pending broker-install resume has fired, so it triggers exactly once. */
    private boolean mPendingResume;
    /** Whether the single-use resume snapshot has already been consumed from the store. */
    private boolean mResumeConsumed;
    /** Request options rebuilt verbatim from the persisted resume snapshot (no UI involvement). */
    private RequestOptions mResumeRequestOptions;
    private INotifyOperationResultCallback<IAuthenticationResult> mAcquireTokenCallback;

    private EditText mAuthority;
    private EditText mLoginhint;
    private Spinner mPrompt;
    private EditText mScope;
    private EditText mExtraScope;
    private EditText mExtraQueryParams;
    private EditText mClaims;
    private Button mAddDeviceIdClaimButton;
    private Button mAddNgcMfaClaimButton;
    private Switch mEnablePII;
    private Switch mForceRefresh;
    private Switch mAllowSignInFromOtherDevice;

    private Button mClearActiveBrokerDiscoveryCache;
    private TextView mCachedActiveBrokerName;
    private Spinner mKnownBrokerApps;
    private Button mSetCachedActiveBrokerButton;
    private Button mGetUsers;
    private Button mClearCache;
    private Button mAcquireToken;
    private Button mAcquireTokenSilent;
    private Button mAcquireTokenWithResource;
    private Button mAcquireTokenSilentWithResource;
    private Button mAcquireTokenWithDeviceCodeFlow;
    private Button mAcquireTokenWithQR;
    private Button mBrokerHelper;
    private Button mGetActiveBrokerPkg;
    private Button mGenerateSHR;
    private Spinner mSelectAccount;
    private Spinner mConfigFileSpinner;
    private Spinner mAuthScheme;
    private TextView mPublicApplicationMode;
    private TextView mDefaultBrowser;
    private TextView mStatus;
    private Button mStatusCopyBtn;
    private Spinner mPopHttpMethod;
    private EditText mPopResourceUrl;
    private EditText mPopClientClaims;
    private LinearLayout mPopSection;
    private LinearLayout mLoginHintSection;
    private ToggleButton mDebugBrokers;
    private Button mPreferredAuthMethod;
    private OnFragmentInteractionListener mOnFragmentInteractionListener;
    private MsalWrapper mMsalWrapper;
    private List<IAccount> mLoadedAccounts = new ArrayList<>();

    // Concurrent execution UI elements
    private EditText mConcurrentCount;
    private EditText mConcurrentIterations;
    private Button mRunConcurrent;
    private LinearLayout mThreadProgressContainer;
    private List<TextView> mThreadProgressViews = new ArrayList<>();
    private List<ConcurrentAcquireTokenExecutor> mRunningExecutors = new ArrayList<>();
    private final Map<Integer, String> mLatestErrors = new HashMap<>();

    private IClientActiveBrokerCache mCache;
    public AcquireTokenFragment() {
        // left empty
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_acquire, container, false);

        mAuthority = view.findViewById(R.id.authority);
        mLoginhint = view.findViewById(R.id.loginHint);
        mPrompt = view.findViewById(R.id.promptBehavior);
        mScope = view.findViewById(R.id.scope);
        mExtraScope = view.findViewById(R.id.extraScope);
        mExtraQueryParams = view.findViewById(R.id.extraQueryParams);
        mClaims = view.findViewById(R.id.claims);
        mAddDeviceIdClaimButton = view.findViewById(R.id.btn_deviceIdClaim);
        mAddDeviceIdClaimButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String str = "{\"access_token\":{\"deviceid\":{\"essential\":true}}}";
                mClaims.setText(str);
            }
        });

        // Force MFA to be done in the last x mins (5? I can't remember the exact number)
        // This is what authapp uses to acquire token for NGC registration.
        // We can use this to test interrupt flow.
        mAddNgcMfaClaimButton = view.findViewById(R.id.btn_ngcMfaClaim);
        mAddNgcMfaClaimButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String str = "{\"access_token\":{\"deviceid\":{\"essential\":true},\"amr\":{\"values\":[\"ngcmfa\"]}}}";
                mClaims.setText(str);
            }
        });

        mEnablePII = view.findViewById(enablePII);
        mForceRefresh = view.findViewById(R.id.forceRefresh);
        mAllowSignInFromOtherDevice =  view.findViewById(R.id.sign_in_from_other_device_switch);
        mSelectAccount = view.findViewById(R.id.select_user);
        mGetUsers = view.findViewById(R.id.btn_getUsers);
        mClearCache = view.findViewById(R.id.btn_clearCache);
        mAcquireToken = view.findViewById(R.id.btn_acquiretoken);
        mAcquireTokenSilent = view.findViewById(R.id.btn_acquiretokensilent);
        mAcquireTokenWithResource = view.findViewById(R.id.btn_acquiretokenWithResource);
        mAcquireTokenSilentWithResource = view.findViewById(R.id.btn_acquiretokensilentWithResource);
        mAcquireTokenWithDeviceCodeFlow = view.findViewById(R.id.btn_acquiretokenWithDeviceCodeFlow);
        mAcquireTokenWithQR = view.findViewById(R.id.btn_acquiretokenWithQR);
        mBrokerHelper = view.findViewById(R.id.btnBrokerHelper);
        mGetActiveBrokerPkg = view.findViewById(R.id.btnGetActiveBroker);
        mGenerateSHR = view.findViewById(R.id.btn_generate_shr);
        mPreferredAuthMethod = view.findViewById(R.id.btnGetPreferredAuthMethod);
        mConfigFileSpinner = view.findViewById(R.id.configFile);
        mAuthScheme = view.findViewById(R.id.authentication_scheme);
        mPublicApplicationMode = view.findViewById(R.id.public_application_mode);
        mDefaultBrowser = view.findViewById(R.id.default_browser);
        mStatus = view.findViewById(R.id.status);
        mStatusCopyBtn = view.findViewById(R.id.btn_statusCopy);
        mPopHttpMethod = view.findViewById(R.id.pop_http_method);
        mPopResourceUrl = view.findViewById(R.id.pop_resource_url);
        mPopClientClaims = view.findViewById(R.id.pop_client_claims);
        mDebugBrokers = view.findViewById(R.id.btn_trust_debug_brkr);
        mDebugBrokers.setTextOff("Prod Brokers");
        mDebugBrokers.setTextOn("Debug Brokers");
        mDebugBrokers.setChecked(BrokerData.getShouldTrustDebugBrokers());

        mCache = ClientActiveBrokerCache.getClientSdkCache(
                AndroidPlatformComponentsFactory.createFromContext(getContext()).getStorageSupplier()
        );

        mCachedActiveBrokerName = view.findViewById(R.id.cachedActiveBrokerName);
        mClearActiveBrokerDiscoveryCache = view.findViewById(R.id.clearActiveBrokerDiscoveryCache);

        mClearActiveBrokerDiscoveryCache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCache.clearCachedActiveBroker();
                setActiveBrokerTextFromCache();
            }
        });

        mKnownBrokerApps = view.findViewById(R.id.spinner_knownBrokerApps);
        mSetCachedActiveBrokerButton = view.findViewById(R.id.btn_setCachedActiveBroker);

        final List<BrokerData> debugBrokers = new ArrayList<>(BrokerData.getDebugBrokers());
        final List<BrokerData> prodBrokers = new ArrayList<>(BrokerData.getProdBrokers());
        bindKnownBrokerAppList(mKnownBrokerApps, debugBrokers, prodBrokers);
        mSetCachedActiveBrokerButton.setOnClickListener(v -> {
            int selectedItem =  mKnownBrokerApps.getSelectedItemPosition();
            if (selectedItem < debugBrokers.size()){
                mCache.setCachedActiveBroker(debugBrokers.get(selectedItem));
            } else {
                mCache.setCachedActiveBroker(prodBrokers.get(selectedItem - debugBrokers.size()));
            }
            setActiveBrokerTextFromCache();
        });


        mPopSection = view.findViewById(R.id.pop_section);
        mLoginHintSection = view.findViewById(R.id.login_hint_section);

        bindSelectAccountSpinner(mSelectAccount, null);
        mSelectAccount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateUiBasedOnCurrentAccount();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        bindSpinnerChoice(mPrompt, Prompt.class);
        bindSpinnerChoice(mAuthScheme, Constants.AuthScheme.class);
        mAuthScheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateUiBasedOnAuthScheme();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        bindSpinnerChoice(mPopHttpMethod, HttpMethod.class);

        bindSpinnerChoice(mConfigFileSpinner, Constants.ConfigFile.class);
        mConfigFileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadMsalApplicationFromRequestParameters(getCurrentRequestOptions());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mStatusCopyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                final ClipData clip = ClipData.newPlainText("MSAL Test App", mStatus.getText());
                clipboard.setPrimaryClip(clip);

                Toast.makeText(getContext(), "Text copied to clipboard.", Toast.LENGTH_LONG).show();
            }
        });

        mAcquireTokenCallback = new INotifyOperationResultCallback<IAuthenticationResult>() {
            @Override
            public void onSuccess(IAuthenticationResult result) {
                mOnFragmentInteractionListener.onGetAuthResult(result);
            }

            @Override
            public void showMessage(String message) {
                AcquireTokenFragment.this.showMessage(message);
            }
        };

        mAcquireToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Span span = OTelUtility.createSpan("TestApp_AcquireToken");
                try (Scope scope = SpanExtension.makeCurrentSpan(span)) {
                    mMsalWrapper.acquireToken(getActivity(), getCurrentRequestOptions(), mAcquireTokenCallback);
                    span.setStatus(StatusCode.OK);
                } catch (final Throwable throwable) {
                    span.recordException(throwable);
                    span.setStatus(StatusCode.ERROR);
                } finally {
                    span.end();
                }
            }
        });

        mAcquireTokenSilent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Span span = OTelUtility.createSpan("TestApp_AcquireTokenSilent");
                try (Scope scope = SpanExtension.makeCurrentSpan(span)) {
                    mMsalWrapper.acquireTokenSilent(getCurrentRequestOptions(), mAcquireTokenCallback);
                    span.setStatus(StatusCode.OK);
                } catch (final Throwable throwable) {
                    span.recordException(throwable);
                    span.setStatus(StatusCode.ERROR);
                } finally {
                    span.end();
                }
            }
        });

        mAcquireTokenWithResource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMsalWrapper.acquireTokenWithResource(getActivity(), getCurrentRequestOptions(), mAcquireTokenCallback);
            }
        });

        mAcquireTokenSilentWithResource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMsalWrapper.acquireTokenSilentWithResource(getCurrentRequestOptions(), mAcquireTokenCallback);
            }
        });

        mAcquireTokenWithDeviceCodeFlow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMsalWrapper.acquireTokenWithDeviceCodeFlow(getCurrentRequestOptions(), mAcquireTokenCallback);
            }
        });

        mAcquireTokenWithQR.setOnClickListener(
                v -> 
                    mMsalWrapper.acquireTokenWithQR(
                        getActivity(),
                        getCurrentRequestOptions(),
                        mAcquireTokenCallback
                ));

        final Activity activity = this.getActivity();
        mBrokerHelper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PublicClientApplication.showExpectedMsalRedirectUriInfo(activity);
            }
        });

        mGetActiveBrokerPkg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String activeBrokerPkgName = mMsalWrapper.getActiveBrokerPkgName(activity);
                final String activeBrokerPkgNameMsg = StringUtil.isNullOrEmpty(activeBrokerPkgName) ? "Could not find a valid broker" : "Active broker pkg name : " + activeBrokerPkgName;
                AcquireTokenFragment.this.showDialog(activeBrokerPkgNameMsg);
            }
        });

        mGetUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnFragmentInteractionListener.onGetUsers(
                        Constants.getResourceIdFromConfigFile(getCurrentRequestOptions().getConfigFile())
                );
            }
        });

        mClearCache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMsalWrapper.removeAccount(
                        getAccountFromSpinner(),
                        new INotifyOperationResultCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                loadAccounts();
                            }

                            @Override
                            public void showMessage(String message) {
                                AcquireTokenFragment.this.showDialog(message);
                            }
                        });
            }
        });

        final INotifyOperationResultCallback<String> generateShrCallback =
                new INotifyOperationResultCallback<String>() {

                    @Override
                    public void onSuccess(String result) {
                        mOnFragmentInteractionListener.onGetStringResult(result);
                    }

                    @Override
                    public void showMessage(String message) {
                        AcquireTokenFragment.this.showMessage(message);
                    }
                };

        mGenerateSHR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMsalWrapper.generateSignedHttpRequest(getCurrentRequestOptions(), generateShrCallback);
            }
        });

        mDebugBrokers.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean debugBrokers) {
                BrokerData.setShouldTrustDebugBrokers(debugBrokers);
            }
        });

        mPreferredAuthMethod.setOnClickListener(v -> AcquireTokenFragment.this.showMessage(
            mMsalWrapper.getPreferredAuthMethod()
        ));

        // Initialize concurrent execution UI elements
        mConcurrentCount = view.findViewById(R.id.concurrent_count);
        mConcurrentIterations = view.findViewById(R.id.concurrent_iterations);
        mRunConcurrent = view.findViewById(R.id.btn_run_concurrent);
        mThreadProgressContainer = view.findViewById(R.id.concurrent_thread_progress_container);

        mRunConcurrent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRunningExecutors.isEmpty()) {
                    runConcurrentAcquireTokenSilent();
                } else {
                    stopConcurrentExecutors();
                }
            }
        });

        updateConcurrentButtonState();

        return view;
    }

    /**
     * POC: when this fragment is created from a broker-install resume deep link, the single-use
     * correlation id is consumed exactly once and the FULL original request is read from the
     * encrypted store, rebuilt into a {@link RequestOptions} verbatim, and auto-fired once the
     * {@link MsalWrapper} finishes loading — matching production controller-level resume rather than
     * reconstructing the request from UI fields. Returns the resume options, or null when there is
     * nothing to resume (no id, or already consumed/expired).
     */
    private RequestOptions resolveResumeRequestOptions() {
        if (mResumeRequestOptions != null) {
            return mResumeRequestOptions;
        }
        if (mResumeConsumed) {
            return null;
        }
        final Bundle args = getArguments();
        final String correlationId = args == null ? null : args.getString(ARG_RESUME_CORRELATION_ID);
        if (correlationId == null) {
            return null;
        }
        // Single-use: consume exactly once, even if expired/missing, to avoid resume loops.
        mResumeConsumed = true;
        final BrokerInstallResumeRequest request = EncryptedBrokerInstallResumeStore
                .create(getContext())
                .consume(correlationId, System.currentTimeMillis());
        android.util.Log.i("ResumePOC", "consume(" + correlationId + ") -> "
                + (request == null
                        ? "NULL (expired / absent / already-consumed)"
                        : "FOUND authority=" + request.getAuthority()
                                + " scopes=" + request.getScopes()
                                + " loginHint=" + request.getLoginHint()));
        if (request == null) {
            showMessage("Broker-install resume window expired");
            return null;
        }
        mResumeRequestOptions = buildResumeRequestOptions(request);
        mPendingResume = true;
        return mResumeRequestOptions;
    }

    /**
     * Rebuilds a {@link RequestOptions} verbatim from the persisted {@link BrokerInstallResumeRequest}
     * snapshot — every interactive parameter (authority, scopes, extra scopes, loginHint, claims,
     * prompt, extra query params) comes from the snapshot, NOT from UI fields. The originating
     * PublicClientApplication is identified by its config; for this WebView-only feature that is the
     * WebView config (analogous to production reusing the original PCA).
     */
    private RequestOptions buildResumeRequestOptions(final BrokerInstallResumeRequest request) {
        return new RequestOptions(
                Constants.ConfigFile.WEBVIEW,
                request.getLoginHint() == null ? "" : request.getLoginHint(),
                null,
                toPrompt(request.getPrompt()),
                joinSpace(request.getScopes()),
                joinSpace(request.getExtraScopesToConsent()),
                request.getExtraQueryParameters() == null ? "" : request.getExtraQueryParameters(),
                request.getClaims() == null ? "" : request.getClaims(),
                false,
                false,
                request.getAuthority() == null ? "" : request.getAuthority(),
                Constants.AuthScheme.BEARER,
                null,
                "",
                "",
                false
        );
    }

    private static String joinSpace(final java.util.List<String> values) {
        return values == null ? "" : android.text.TextUtils.join(" ", values);
    }

    private static Prompt toPrompt(final String raw) {
        if (raw == null) {
            return Prompt.WHEN_REQUIRED;
        }
        switch (raw.toLowerCase(java.util.Locale.ROOT)) {
            case "login":
                return Prompt.LOGIN;
            case "consent":
                return Prompt.CONSENT;
            case "select_account":
                return Prompt.SELECT_ACCOUNT;
            default:
                return Prompt.WHEN_REQUIRED;
        }
    }

    private void setActiveBrokerTextFromCache() {
        final BrokerData activeBroker = mCache.getCachedActiveBroker();
        mCachedActiveBrokerName.setText(activeBroker == null ? "none" : activeBroker.getPackageName());
    }

    private void updateUiBasedOnAuthScheme() {
        final Constants.AuthScheme authScheme = Constants.AuthScheme.valueOf(mAuthScheme.getSelectedItem().toString());
        if (authScheme == Constants.AuthScheme.POP) {
            mPopSection.setVisibility(View.VISIBLE);
            mGenerateSHR.setVisibility(View.VISIBLE);
        } else {
            mPopSection.setVisibility(View.GONE);
            mGenerateSHR.setVisibility(View.GONE);
        }
    }

    // If an account is selected.
    //  - Hide loginhint section.
    //  - Set hint in mAuthority.
    private void updateUiBasedOnCurrentAccount() {
        final IAccount account = getAccountFromSpinner();
        if (account == null) {
            mLoginhint.setCursorVisible(true);
            mLoginhint.setEnabled(true);
            mLoginHintSection.setVisibility(View.VISIBLE);
            mAuthority.setHint("");
        } else {
            mLoginhint.setText("");
            mLoginhint.setCursorVisible(false);
            mLoginhint.setEnabled(false);
            mLoginHintSection.setVisibility(View.GONE);
            mAuthority.setHint("Default: account's authority");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnFragmentInteractionListener) {
            mOnFragmentInteractionListener = (OnFragmentInteractionListener) context;
        } else {
            throw new IllegalStateException("OnFragmentInteractionListener is not implemented");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        final RequestOptions resumeOptions = resolveResumeRequestOptions();
        loadMsalApplicationFromRequestParameters(
                resumeOptions != null ? resumeOptions : getCurrentRequestOptions());
        setActiveBrokerTextFromCache();
    }

    private void loadAccounts() {
        if (mMsalWrapper != null) {
            mMsalWrapper.loadAccounts(new INotifyOperationResultCallback<List<IAccount>>() {
                @Override
                public void onSuccess(List<IAccount> result) {
                    mLoadedAccounts = result;
                    refreshUI();
                }

                @Override
                public void showMessage(String message) {
                    AcquireTokenFragment.this.showMessage(message);
                }
            });
        }
    }

    private void refreshUI() {
        bindSelectAccountSpinner(mSelectAccount, mLoadedAccounts);
        updateUiBasedOnAuthScheme();
        mPublicApplicationMode.setText(mMsalWrapper.getMode());
        mDefaultBrowser.setText(mMsalWrapper.getDefaultBrowser());
    }

    private IAccount getAccountFromSpinner() {
        if (mLoadedAccounts == null || mLoadedAccounts.isEmpty()) {
            return null;
        }

        int selectedAccountPosition = mSelectAccount.getSelectedItemPosition();

        // This means that there is no selected account.
        if (selectedAccountPosition == AdapterView.INVALID_POSITION) {
            return null;
        }

        // We're using the last tile to display "-- No Account Selected --"
        if (selectedAccountPosition == mLoadedAccounts.size()) {
            return null;
        }

        return mLoadedAccounts.get(selectedAccountPosition);
    }

    private void bindKnownBrokerAppList(final Spinner spinner,
                                        final List<BrokerData> debugBrokers,
                                        final List<BrokerData> prodBrokers) {
        final ArrayAdapter<String> userAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<String>() {{
                    for (BrokerData brokerData: debugBrokers) {
                        add("[DEBUG]"+ brokerData.getPackageName());
                    }
                    for (BrokerData brokerData: prodBrokers) {
                        add("[PROD]"+ brokerData.getPackageName());
                    }
                }}
        );
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(userAdapter);
        spinner.setSelection(0, false);
    }

    private void bindSelectAccountSpinner(final Spinner spinner,
                                          final List<IAccount> accounts) {
        final ArrayAdapter<String> userAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<String>() {{
                    if (accounts != null) {
                        for (IAccount account : accounts) {
                            add(account.getUsername());
                        }
                    }
                    add("-- No Account Selected --");
                }}
        );
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(userAdapter);
        spinner.setSelection(0, false);
    }

    private void bindSpinnerChoice(final Spinner spinner, final Class<? extends Enum> spinnerChoiceClass) {
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<String>() {{
                    for (Enum choice : spinnerChoiceClass.getEnumConstants()) {
                        add(choice.name());
                    }

                    if (spinnerChoiceClass.isAssignableFrom(HttpMethod.class)) {
                        // Add 1 more option for "none"
                        add(NONE_NULL);
                    }
                }}
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0, false);
    }

    private RequestOptions getCurrentRequestOptions() {
        final Constants.ConfigFile configFile = Constants.ConfigFile.valueOf(mConfigFileSpinner.getSelectedItem().toString());
        final String loginHint = mLoginhint.getText().toString();
        final IAccount account = getAccountFromSpinner();
        final Prompt promptBehavior = Prompt.valueOf(mPrompt.getSelectedItem().toString());
        final String scopes = mScope.getText().toString();
        final String extraScopesToConsent = mExtraScope.getText().toString();
        final String extraQueryParametersField = mExtraQueryParams.getText().toString();
        final String claims = mClaims.getText().toString();
        final boolean enablePII = mEnablePII.isChecked();
        final boolean forceRefresh = mForceRefresh.isChecked();
        final boolean allowSignInFromOtherDevice = mAllowSignInFromOtherDevice.isChecked();
        final String authority = mAuthority.getText().toString();
        final Constants.AuthScheme authScheme = Constants.AuthScheme.valueOf(mAuthScheme.getSelectedItem().toString());
        final String httpMethodTextFromSpinner = mPopHttpMethod.getSelectedItem().toString();
        final HttpMethod popHttpMethod = httpMethodTextFromSpinner.equals(NONE_NULL)
                ? null // None specified
                : HttpMethod.valueOf(httpMethodTextFromSpinner);
        final String popResourceUrl = mPopResourceUrl.getText().toString();
        final String popClientClaimsTxt = mPopClientClaims.getText().toString();

        return new RequestOptions(
                configFile,
                loginHint,
                account,
                promptBehavior,
                scopes,
                extraScopesToConsent,
                extraQueryParametersField,
                claims,
                enablePII,
                forceRefresh,
                authority,
                authScheme,
                popHttpMethod,
                popResourceUrl,
                popClientClaimsTxt,
                allowSignInFromOtherDevice
        );
    }

    private void loadMsalApplicationFromRequestParameters(final RequestOptions requestOptions) {
        boolean enablePiiLogging = requestOptions.isEnablePII();
        // The sample app is having the PII enable setting on the MainActivity. Ideally, app should decide to enable Pii or not,
        // if it's enabled, it should be set when the application is onCreate.
        Logger.getInstance().setEnableLogcatLog(enablePiiLogging);
        if (enablePiiLogging) {
            Logger.getInstance().setEnablePII(true);
        } else {
            Logger.getInstance().setEnablePII(false);
        }

        MsalWrapper.create(getContext(),
                Constants.getResourceIdFromConfigFile(requestOptions.getConfigFile()),
                new INotifyOperationResultCallback<MsalWrapper>() {
                    @Override
                    public void onSuccess(MsalWrapper result) {
                        mMsalWrapper = result;
                        loadAccounts();
                        if (mPendingResume) {
                            mPendingResume = false;
                            android.util.Log.i("ResumePOC", "AUTO-FIRING interactive acquireToken from persisted resume snapshot (broker webview config)");
                            showMessage("Auto-resuming sign-in after broker install");
                            mMsalWrapper.acquireToken(getActivity(), mResumeRequestOptions, mAcquireTokenCallback);
                        }
                    }

                    @Override
                    public void showMessage(String message) {
                        AcquireTokenFragment.this.showMessage(message);
                    }
                });
    }

    private void showMessage(final String msg) {
        new Handler(getActivity().getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                mStatus.setText(msg);
            }
        });
    }

    private void showDialog(final String msg) {
        new Handler(getActivity().getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.custom_dialog_layout, null);
                builder.setView(dialogView);

                TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
                Button okButton = dialogView.findViewById(R.id.dialog_ok_button);
                dialogMessage.setText(msg);

                AlertDialog dialog = builder.create();
                okButton.setOnClickListener(v -> {
                    dialog.dismiss();
                });

                dialog.show();
            }
        });
    }

    /**
     * Concurrent AcquireTokenSilent
     */
    private void runConcurrentAcquireTokenSilent() {
        try {
            final int concurrency = Integer.parseInt(mConcurrentCount.getText().toString());
            final int iterations = Integer.parseInt(mConcurrentIterations.getText().toString());

            if (concurrency <= 0 || iterations <= 0) {
                showConcurrentStatus("Concurrency and iterations must be greater than 0");
                return;
            }

            if (getAccountFromSpinner() == null) {
                showConcurrentStatus("Please sign in first. An account is required for AcquireTokenSilent.");
                return;
            }

            // Reset progress
            mThreadProgressContainer.removeAllViews();
            mThreadProgressViews.clear();
            mLatestErrors.clear();

            // Create per-thread progress views
            final TextView header = new TextView(getContext());
            header.setText("Progress (success/completed):");
            header.setTextSize(12);
            header.setTypeface(null, android.graphics.Typeface.BOLD);
            header.setPadding(0, 5, 0, 5);
            mThreadProgressContainer.addView(header);

            for (int i = 0; i < concurrency; i++) {
                TextView threadProgress = new TextView(getContext());
                threadProgress.setText("Thread " + i + ": 0/" + iterations);
                threadProgress.setTextSize(12);
                threadProgress.setPadding(0, 5, 0, 5);
                mThreadProgressContainer.addView(threadProgress);
                mThreadProgressViews.add(threadProgress);
            }

            // Set up a shared CyclicBarrier so all executor threads synchronize
            // on each iteration. This ensures all N threads fire their ATS request
            // at the exact same moment on each wave.
            ConcurrentAcquireTokenExecutor.setSharedBarrier(
                    new java.util.concurrent.CyclicBarrier(concurrency));

            // Create and start one executor per thread
            for (int i = 0; i < concurrency; i++) {
                final int threadId = i;

                final ConcurrentAcquireTokenExecutor executor =
                    new ConcurrentAcquireTokenExecutor(threadId, iterations);

                // Each executor fires requests on its own background thread.
                // The shared CyclicBarrier ensures all executors synchronize at each
                // iteration, so all threads fire simultaneously on each wave.
                executor.execute(
                    getContext(),
                    getCurrentRequestOptions(),
                    new ConcurrentAcquireTokenExecutor.IUIUpdateCallback() {
                        @Override
                        public void updateProgress(final int tid, final int successCount, final int completedCount) {
                            if (tid >= 0 && tid < mThreadProgressViews.size()) {
                                final String progress = String.format(Locale.US,
                                        "Thread %d [%s]: %d/%d",
                                        tid,
                                        ConcurrentAcquireTokenExecutor.getScopeForThread(tid),
                                        successCount,
                                        completedCount);

                                if (mLatestErrors.containsKey(tid)) {
                                    final String full = progress + "\nLast error: " + mLatestErrors.get(tid);
                                    final android.text.SpannableString spannable = new android.text.SpannableString(full);
                                    final int boldStart = progress.length() + 1; // after \n
                                    final int boldEnd = boldStart + "Last error:".length();
                                    spannable.setSpan(
                                            new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                                            boldStart, boldEnd,
                                            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    mThreadProgressViews.get(tid).setText(spannable);
                                } else {
                                    mThreadProgressViews.get(tid).setText(progress);
                                }
                            }
                        }

                        @Override
                        public void onStopped(final int tid) {
                            mRunningExecutors.removeIf(e -> e.getThreadId() == tid);
                            if (mRunningExecutors.isEmpty()) {
                                updateConcurrentButtonState();
                            }
                        }

                        @Override
                        public void onError(final int tid, final String message) {
                            mLatestErrors.put(tid, message);
                        }
                    }
                );

                mRunningExecutors.add(executor);
            }
            updateConcurrentButtonState();
        } catch (NumberFormatException e) {
            showMessage("Please enter valid numbers for concurrency and iterations");
        }
    }

    private void stopConcurrentExecutors() {
        if (mRunningExecutors.isEmpty()) {
            return;
        }
        for (ConcurrentAcquireTokenExecutor executor : mRunningExecutors) {
            executor.stop();
        }
        mRunningExecutors.clear();
        // Append stop message without clearing existing thread progress
        final TextView stopView = new TextView(getContext());
        stopView.setText("Stopped all running tasks");
        stopView.setTextSize(12);
        stopView.setPadding(0, 10, 0, 5);
        stopView.setTextColor(0xFF999999);
        mThreadProgressContainer.addView(stopView);
        updateConcurrentButtonState();
    }

    private void updateConcurrentButtonState() {
        final boolean hasRunning = !mRunningExecutors.isEmpty();
        mRunConcurrent.setText(hasRunning ? "Stop" : "Run Concurrent");
    }

    private void showConcurrentStatus(final String message) {
        mThreadProgressContainer.removeAllViews();
        mThreadProgressViews.clear();
        final TextView statusView = new TextView(getContext());
        statusView.setText(message);
        statusView.setTextSize(14);
        statusView.setPadding(0, 10, 0, 10);
        statusView.setTextColor(0xFFCC0000);
        mThreadProgressContainer.addView(statusView);
    }

    public interface OnFragmentInteractionListener {
        void onGetAuthResult(IAuthenticationResult result);

        void onGetStringResult(String valueToDisplay);

        void onGetUsers(int configFileResourceId);
    }
}
