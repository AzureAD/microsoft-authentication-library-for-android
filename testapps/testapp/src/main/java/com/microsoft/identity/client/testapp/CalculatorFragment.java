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

public class CalculatorFragment extends Fragment {

    private EditText firstInput, secondInput;
    private TextView resultTextView;

    private Button add, subtract, multiply, divide;

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

        add = view.findViewById(R.id.add);
        subtract = view.findViewById(R.id.subtract);
        multiply = view.findViewById(R.id.multiply);
        divide = view.findViewById(R.id.divide);

        return view;
    }
}
