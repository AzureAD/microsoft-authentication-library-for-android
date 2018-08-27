package com.microsoft.identity.client;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PublicClientConfigurationTest {

    /**
     * Verifies that a minimum configuration containing only:
     * client_id
     * redirect_uri
     *
     * Is merged with the default configuration correctly.
     */
    @Test
    public void testMinimumValidConfiguration(){

    }

    /**
     * Verify B2C Authority set via configuration correctly
     */
    @Test
    public void testB2CAuthorityValidConfiguration(){

    }

    /**
     * Verify that unknown authority type results in exception
     */
    @Test
    public void testUnknownAuthorityException(){

    }

    /**
     * Verify that unknown audience type results in exception
     */
    @Test
    public void testUnknownAudienceException(){

    }

    /**
     * Verify that null client id throws an exception
     */
    @Test
    public void testNullClientIdException(){

    }

    /**
     * Verify that null redirect URI throws an exception
     */
    @Test
    public void testNullRedirectUrlException(){

    }


}
