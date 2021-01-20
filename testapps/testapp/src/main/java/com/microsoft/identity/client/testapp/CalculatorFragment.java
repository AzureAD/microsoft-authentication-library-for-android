package com.microsoft.identity.client.testapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.microsoft.identity.client.CalculationCallback;
import com.microsoft.identity.client.CalculationParameters;
import com.microsoft.identity.common.internal.util.StringUtil;

public class CalculatorFragment extends Fragment {

    private EditText firstInput, secondInput;
    private TextView resultTextView, errorTextView;
    private static final String TAG = CalculatorFragment.class.getSimpleName();

    private Button add, subtract, multiply, divide;

    private MsalWrapper mMsalWrapper;

    private CalculationCallback calculationCallback = new CalculationCallback() {
        @Override
        public void onError(Exception exception) {
            errorTextView.setText(String.format("Error: %s", exception.getMessage()));
            resultTextView.setText("");
        }

        @Override
        public void onCancel() {
            errorTextView.setText(R.string.error_operation_cancelled);
        }

        @Override
        public void onTaskCompleted(String s) {
            errorTextView.setText("");
            resultTextView.setText(s);
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calculator, container, false);

        firstInput = view.findViewById(R.id.firstInput);
        secondInput = view.findViewById(R.id.secondInput);

        resultTextView = view.findViewById(R.id.result);
        errorTextView = view.findViewById(R.id.error);

        add = view.findViewById(R.id.add);
        subtract = view.findViewById(R.id.subtract);
        multiply = view.findViewById(R.id.multiply);
        divide = view.findViewById(R.id.divide);

        divide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!StringUtil.isEmpty(firstInput.getText().toString()) && !StringUtil.isEmpty(secondInput.getText().toString())) {

                    float first = Float.parseFloat(firstInput.getText().toString());
                    float second = Float.parseFloat(secondInput.getText().toString());

                    mMsalWrapper.calculate(new CalculationParameters.Builder().withCallback(calculationCallback).divide(first, second).build());
                }
            }
        });

        multiply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!StringUtil.isEmpty(firstInput.getText().toString()) && !StringUtil.isEmpty(secondInput.getText().toString())) {

                    float first = Float.parseFloat(firstInput.getText().toString());
                    float second = Float.parseFloat(secondInput.getText().toString());

                    mMsalWrapper.calculate(new CalculationParameters.Builder().withCallback(calculationCallback).multiply(first, second).build());
                }
            }
        });

        subtract.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!StringUtil.isEmpty(firstInput.getText().toString()) && !StringUtil.isEmpty(secondInput.getText().toString())) {

                    float first = Float.parseFloat(firstInput.getText().toString());
                    float second = Float.parseFloat(secondInput.getText().toString());

                    mMsalWrapper.calculate(new CalculationParameters.Builder().withCallback(calculationCallback).subtract(first, second).build());
                }
            }
        });

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!StringUtil.isEmpty(firstInput.getText().toString()) && !StringUtil.isEmpty(secondInput.getText().toString())) {

                    float first = Float.parseFloat(firstInput.getText().toString());
                    float second = Float.parseFloat(secondInput.getText().toString());

                    mMsalWrapper.calculate(new CalculationParameters.Builder().withCallback(calculationCallback).add(first, second).build());
                }
            }
        });

        loadMsalApplicationFromRequestParameters();
        return view;
    }

    private void loadMsalApplicationFromRequestParameters() {
        MsalWrapper.create(getContext(),
                Constants.getResourceIdFromConfigFile(Constants.ConfigFile.DEFAULT),
                new INotifyOperationResultCallback<MsalWrapper>() {
                    @Override
                    public void onSuccess(MsalWrapper result) {
                        mMsalWrapper = result;
                    }

                    @Override
                    public void showMessage(String message) {
                    }
                });
    }
}
