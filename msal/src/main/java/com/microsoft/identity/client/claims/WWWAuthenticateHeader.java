package com.microsoft.identity.client.claims;

public class WWWAuthenticateHeader {

    static final String CLAIMS_DIRECTIVE = "claims=";
    static final char SINGLE_QUOTE = '\'';
    static final char DOUBLE_QUOTE = '"';
    static final char SPACE = ' ';
    static final char COMMA = ',';

    public static ClaimsRequest getClaimsRequestFromWWWAuthenticateHeaderValue(String headerValue){

        int claimsDirectiveIndex = headerValue.indexOf(CLAIMS_DIRECTIVE);
        String claimsDirectiveJsonString = null;

        if(claimsDirectiveIndex == -1){
            //Not Found
            return null;
        }

        //Check if directive surrounded by quotes (single or double)
        int valueStartPosition = claimsDirectiveIndex + CLAIMS_DIRECTIVE.length();
        int valueEndPosition = 0;
        int valueEndComma = 0;
        char openQuote = headerValue.substring(valueStartPosition, valueStartPosition + 1).charAt(0);


        if(openQuote == SINGLE_QUOTE){
            //Let's get the claims string
            valueEndPosition = headerValue.indexOf(SINGLE_QUOTE, valueStartPosition +1);
            claimsDirectiveJsonString = headerValue.substring(valueStartPosition + 1, valueEndPosition);
        }else if(openQuote == DOUBLE_QUOTE){
            valueEndPosition = headerValue.indexOf("}" +DOUBLE_QUOTE, valueStartPosition +1);
            claimsDirectiveJsonString = headerValue.substring(valueStartPosition + 1, valueEndPosition +1);
        }else{
            valueEndComma = headerValue.indexOf(COMMA, valueStartPosition);
            valueEndPosition = headerValue.indexOf(SPACE, valueStartPosition);
            if(valueEndComma != -1 || valueEndPosition != -1 ){
                if(valueEndComma != -1) {
                    claimsDirectiveJsonString = headerValue.substring(valueStartPosition, valueEndComma);
                }else{
                    claimsDirectiveJsonString = headerValue.substring(valueStartPosition, valueEndPosition);
                }
            }else{
                claimsDirectiveJsonString = headerValue.substring(valueStartPosition);
            }
        }

        return ClaimsRequest.getClaimsRequestFromJsonString(claimsDirectiveJsonString);
    }



    public static Boolean hasClaimsDirective(String headerValue){
        int claimsDirectiveIndex = headerValue.indexOf(CLAIMS_DIRECTIVE);

        if(claimsDirectiveIndex == -1){
            return false;
        }

        return true;
    }

}
