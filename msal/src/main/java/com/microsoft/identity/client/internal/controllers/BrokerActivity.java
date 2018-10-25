package com.microsoft.identity.client.internal.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public final class BrokerActivity extends Activity {

    public static final String BROKER_INTENT = "broker_intent";
    static final String BROKER_INTENT_STARTED = "broker_intent_started";
    static final int BROKER_INTENT_REQUEST_CODE = 1001;

    private static final String TAG = BrokerActivity.class.getSimpleName();

    private Intent mBrokerInteractiveRequestIntent;
    private Boolean mBrokerIntentStarted = false;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState == null) {
            mBrokerInteractiveRequestIntent = getIntent().getExtras().getParcelable(BROKER_INTENT);
        }else{
            mBrokerInteractiveRequestIntent = savedInstanceState.getParcelable(BROKER_INTENT);
            mBrokerIntentStarted = savedInstanceState.getBoolean(BROKER_INTENT_STARTED);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!mBrokerIntentStarted){
            mBrokerIntentStarted = true;
            startActivityForResult(mBrokerInteractiveRequestIntent, BROKER_INTENT_REQUEST_CODE);
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(BROKER_INTENT, mBrokerInteractiveRequestIntent);
        outState.putBoolean(BROKER_INTENT_STARTED, mBrokerIntentStarted);
    }

    /**
     * Receive result from broker intent
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){

        //Todo: Need the possible resultCodes that could be returned....
        if(requestCode == BROKER_INTENT_REQUEST_CODE){
            MSALApiDispatcher.completeInteractive(requestCode, resultCode, data);
        }
    }


}
