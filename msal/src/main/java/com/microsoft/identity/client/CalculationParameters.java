package com.microsoft.identity.client;

public class CalculationParameters {

    private final CalculationCallback mCallback;
    private final float mFirst;
    private final float mSecond;
    private final char mOperator;

    public CalculationParameters(CalculationParameters.Builder builder) {
        this.mCallback = builder.mCallback;
        this.mFirst = builder.mFirst;
        this.mSecond = builder.mSecond;
        this.mOperator = builder.mOperator;
    }

    public CalculationCallback getCallback() {
        return mCallback;
    }

    public char getOperator() {
        return mOperator;
    }

    public float getFirst() {
        return mFirst;
    }

    public float getSecond() {
        return mSecond;
    }


    public static class Builder {

        private CalculationCallback mCallback;
        private float mFirst;
        private float mSecond;
        private char mOperator;

        public CalculationParameters.Builder evaluate(float first, float second, char operator) {
            this.mFirst = first;
            this.mSecond = second;
            this.mOperator = operator;
            return this;
        }


        public CalculationParameters.Builder add(float first, float second) {
            return this.evaluate(first, second, '+');
        }

        public CalculationParameters.Builder subtract(float first, float second) {
            return this.evaluate(first, second, '-');
        }

        public CalculationParameters.Builder divide(float first, float second) {
            return this.evaluate(first, second, '/');
        }

        public CalculationParameters.Builder multiply(float first, float second) {
            return this.evaluate(first, second, '*');
        }

        public CalculationParameters.Builder withCallback(CalculationCallback callback) {
            this.mCallback = callback;
            return this;
        }



        public CalculationParameters build(){
            return new CalculationParameters(this);
        }
    }
}
