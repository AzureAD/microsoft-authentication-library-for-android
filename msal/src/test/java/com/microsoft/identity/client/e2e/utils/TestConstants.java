package com.microsoft.identity.client.e2e.utils;

public class TestConstants {

    public static class Configurations {
        public static final String B2C_CONFIG_FILE_PATH = "src/test/res/raw/b2c_test_config.json";
        public static final String MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH = "src/test/res/raw/multiple_account_aad_test_config.json";
        public static final String SINGLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH = "src/test/res/raw/single_account_aad_test_config.json";
    }

    public static class Scopes {
        public static final String[] USER_READ_SCOPE = {"user.read"};
        public static final String[] B2C_SCOPE = {"https://msidlabb2c.onmicrosoft.com/msidlabb2capi/read"};
    }

    public static class Authorities {
        public static final String AAD_MOCK_AUTHORITY = "https://test.authority/mock";
        public static final String AAD_MOCK_DELAYED_RESPONSE_AUTHORITY = "https://test.authority/mock_with_delays";
    }

}
