package com.microsoft.identity.client;

import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.claims.WWWAuthenticateHeader;

import junit.framework.Assert;

import org.junit.Test;

public class WWWAuthenticateHeaderTest {

    public final static String HEADER_SINGLE_QUOTE = "realm='', claims='{\"access_token\":{\"device_id\":null}}', realm=''";
    public final static String HEADER_DOUBLE_QUOTE = "realm=\"\", claims=\"{\"access_token\":{\"device_id\":null}}\", realm=\"\"";
    public final static String HEADER_NOQUOTE = "realm=, claims={\"access_token\":{\"device_id\":null}}, realm=";
    public final static String NO_CLAIMS_DIRECTIVE = "realm=\"\" ";
    public final static String DEVICE_ID_CLAIM_NAME = "device_id";
    public final static String NULL_ADDITIONAL_INFO = null;

    @Test
    public void testHasClaimsDirective(){

        boolean result = WWWAuthenticateHeader.hasClaimsDirective(HEADER_SINGLE_QUOTE);
        Assert.assertEquals(true, result);
        
    }

    @Test
    public void testDoesNotHaveClaimsDirective(){

        boolean result = WWWAuthenticateHeader.hasClaimsDirective(NO_CLAIMS_DIRECTIVE);
        Assert.assertEquals(false, result);

    }

    @Test
    public void testGetClaimsRequestFromHeaderSingleQuoted(){

        ClaimsRequest claimsRequest = WWWAuthenticateHeader.getClaimsRequestFromWWWAuthenticateHeaderValue(HEADER_SINGLE_QUOTE);

        Assert.assertEquals(DEVICE_ID_CLAIM_NAME, claimsRequest.getAccessTokenClaimsRequested().get(0).getName());
        Assert.assertEquals(NULL_ADDITIONAL_INFO, claimsRequest.getAccessTokenClaimsRequested().get(0).getAdditionalInformation());
    }

    @Test
    public void testGetClaimsRequestFromHeaderDoubleQuoted(){
        ClaimsRequest claimsRequest = WWWAuthenticateHeader.getClaimsRequestFromWWWAuthenticateHeaderValue(HEADER_DOUBLE_QUOTE);

        Assert.assertEquals(DEVICE_ID_CLAIM_NAME, claimsRequest.getAccessTokenClaimsRequested().get(0).getName());
        Assert.assertEquals(NULL_ADDITIONAL_INFO, claimsRequest.getAccessTokenClaimsRequested().get(0).getAdditionalInformation());
    }

    @Test
    public void testGetClaimsRequestFromHeaderNoQuotes(){

        ClaimsRequest claimsRequest = WWWAuthenticateHeader.getClaimsRequestFromWWWAuthenticateHeaderValue(HEADER_NOQUOTE);

        Assert.assertEquals(DEVICE_ID_CLAIM_NAME, claimsRequest.getAccessTokenClaimsRequested().get(0).getName());
        Assert.assertEquals(NULL_ADDITIONAL_INFO, claimsRequest.getAccessTokenClaimsRequested().get(0).getAdditionalInformation());
    }







}
