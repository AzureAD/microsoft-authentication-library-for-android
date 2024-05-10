package com.microsoft.identity.client.e2e.tests.network.nativeauth

import com.microsoft.identity.internal.testutils.TestConstants.Configurations.NATIVE_AUTH_SIGN_IN_TEST_CONFIG_FILE_PATH
import com.microsoft.identity.nativeauth.statemachine.errors.SignInError
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class SignInTest : NativeAuthPublicClientApplicationAbstractTest() {

    override fun getConfigFilePath(): String = NATIVE_AUTH_SIGN_IN_TEST_CONFIG_FILE_PATH

    @Test
    fun testSignInSimple() = runTest {
        // TODO set up LabsUserHelper - requires password to be moved to keyvault
        val result = application.signIn("nativeauthuser1@1secmail.org", "fakepassword".toCharArray())
        Assert.assertTrue(result is SignInError)
        Assert.assertTrue((result as SignInError).isInvalidCredentials())
    }
}