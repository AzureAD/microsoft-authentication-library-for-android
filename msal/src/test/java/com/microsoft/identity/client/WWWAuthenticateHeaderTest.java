package com.microsoft.identity.client;

import org.junit.Test;

public class WWWAuthenticateHeaderTest {

    public final static String HEADER_SINGLE_QUOTE = "realm='', claims='{\"access_token\":{\"device_id\":null}}'";
    public final static String HEADER_DOUBLE_QUOTE = "realm=\"\", claims=\"{\"access_token\":{\"device_id\":null}}\"";
    public final static String HEADER_NOQUOTE = "realm=, claims={\"access_token\":{\"device_id\":null}}";

    @Test
    public void testSingleQuoteWWWAuthenticateHeader(){

        //WWWAuthenticateHeader.hasClaimsDirective()
        
    }







}
