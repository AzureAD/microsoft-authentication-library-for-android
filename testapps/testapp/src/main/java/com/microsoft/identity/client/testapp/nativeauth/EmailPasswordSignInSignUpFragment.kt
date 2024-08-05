//   Copyright (c) Microsoft Corporation.
//   All rights reserved.
//
//   This code is licensed under the MIT License.
//
//   Permission is hereby granted, free of charge, to any person obtaining a copy
//   of this software and associated documentation files(the "Software"), to deal
//   in the Software without restriction, including without limitation the rights
//   to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//   copies of the Software, and to permit persons to whom the Software is
//   furnished to do so, subject to the following conditions :
//
//   The above copyright notice and this permission notice shall be included in
//   all copies or substantial portions of the Software.
//
//   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//   THE SOFTWARE.
package com.microsoft.identity.client.testapp.nativeauth

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.microsoft.identity.client.Account
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.testapp.Constants
import com.microsoft.identity.client.testapp.R
import com.microsoft.identity.client.testapp.databinding.FragmentEmailPasswordBinding
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.statemachine.errors.SignInError
import com.microsoft.identity.nativeauth.statemachine.results.GetAccessTokenResult
import com.microsoft.identity.nativeauth.statemachine.results.GetAccountResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.statemachine.results.SignOutResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult
import com.microsoft.identity.nativeauth.statemachine.states.AccountState
import com.microsoft.identity.nativeauth.statemachine.states.SignInContinuationState
import com.microsoft.identity.nativeauth.statemachine.states.SignUpCodeRequiredState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fragment used for the email and password sign up and sign in flow.
 */
class EmailPasswordSignInSignUpFragment : Fragment() {

    private lateinit var authClient: INativeAuthPublicClientApplication
    private var _binding: FragmentEmailPasswordBinding? = null
    private val binding get() = _binding!!

    companion object {
        private enum class STATUS { SignedIn, SignedOut }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEmailPasswordBinding.inflate(inflater, container, false)
        val view = binding.root

        authClient = AuthClient.getAuthClient()

        init()

        return view
    }

    override fun onResume() {
        super.onResume()
        getStateAndUpdateUI()
    }

    private fun init() {
        initializeButtonListeners()
    }

    private fun initializeButtonListeners() {
        binding.signIn.setOnClickListener {
            signIn()
        }

        binding.signUp.setOnClickListener {
            signUp()
        }

        binding.signOut.setOnClickListener {
            signOut()
        }
    }
    private fun getStateAndUpdateUI() {
        CoroutineScope(Dispatchers.Main).launch {
            val accountResult = authClient.getCurrentAccount()
            when (accountResult) {
                is GetAccountResult.AccountFound -> {
                    displaySignedInState(accountResult.resultValue)
                }
                is GetAccountResult.NoAccountFound -> {
                    displaySignedOutState()
                }
            }
        }
    }
    private fun signIn() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val email = binding.emailText.text.toString()
                val password = CharArray(binding.passwordText.length());
                binding.passwordText.text?.getChars(0, binding.passwordText.length(), password, 0);

                val actionResult = authClient.signIn(
                    username = email,
                    password = password,
                    scopes = listOf("User.Read")
                )

                password.fill('0');

                when (actionResult) {
                    is SignInResult.Complete -> {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.sign_in_successful_message),
                            Toast.LENGTH_SHORT
                        ).show()
                        displaySignedInState(accountState = actionResult.resultValue)
                    }
                    is SignInError -> {
                        if (actionResult.isBrowserRequired()) {
                            Toast.makeText(requireContext(), actionResult.errorMessage, Toast.LENGTH_SHORT).show()

                            authClient.acquireToken(
                                AcquireTokenParameters(
                                    AcquireTokenParameters.Builder()
                                        .startAuthorizationFromActivity(requireActivity())
                                        .withScopes(mutableListOf("profile", "openid", "email"))
                                        .withCallback(getAuthInteractiveCallback())
                                )
                            )
                        }
                        else {
                            displayDialog("Unexpected result", actionResult.errorMessage)
                        }
                    }
                    else -> {
                        displayDialog( "Unexpected result", actionResult.toString())
                    }
                }
            } catch (exception: MsalException) {
                displayDialog(getString(R.string.msal_exception_title), exception.message.toString())
            }
        }
    }

    private fun signUp() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val email = binding.emailText.text.toString()
                val password = CharArray(binding.passwordText.length());
                binding.passwordText.text?.getChars(0, binding.passwordText.length(), password, 0);

                val actionResult = authClient.signUp(
                    username = email,
                    password = password
                )

                password.fill('0')

                when (actionResult) {
                    is SignUpResult.CodeRequired -> {
                        navigateToSignUp(
                            nextState = actionResult.nextState,
                            codeLength =  actionResult.codeLength,
                            sentTo = actionResult.sentTo,
                            channel = actionResult.channel
                        )
                    }
                    is SignUpResult.Complete -> {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.sign_up_successful_message),
                            Toast.LENGTH_SHORT
                        ).show()
                        signInAfterSignUp(
                            nextState = actionResult.nextState
                        )
                    }
                    else -> {
                        displayDialog("Unexpected result", actionResult.toString())
                    }
                }
            } catch (exception: MsalException) {
                displayDialog(getString(R.string.msal_exception_title), exception.message.toString())
            }
        }
    }

    private suspend fun signInAfterSignUp(nextState: SignInContinuationState) {
        val currentState = nextState
        val actionResult = currentState.signIn()
        when (actionResult) {
            is SignInResult.Complete -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.sign_in_successful_message),
                    Toast.LENGTH_SHORT
                ).show()
                displaySignedInState(accountState = actionResult.resultValue)
            }
            else -> {
                displayDialog("Unexpected result", actionResult.toString())
            }
        }
    }
    private fun signOut() {
        CoroutineScope(Dispatchers.Main).launch {
            val getAccountResult = authClient.getCurrentAccount()
            if (getAccountResult is GetAccountResult.AccountFound) {
                val signOutResult = getAccountResult.resultValue.signOut()
                if (signOutResult is SignOutResult.Complete) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.sign_out_successful_message),
                        Toast.LENGTH_SHORT
                    ).show()
                    displaySignedOutState()
                } else {
                    displayDialog("Unexpected result", signOutResult.toString())
                }
            }
        }
    }
    private fun displaySignedInState(accountState: AccountState) {
        emptyFields()
        updateUI(STATUS.SignedIn)
        displayAccount(accountState)
    }

    private fun displaySignedOutState() {
        emptyFields()
        updateUI(STATUS.SignedOut)
        emptyResults()
    }
    private fun updateUI(status: STATUS) {
        when (status) {
            STATUS.SignedIn -> {
                binding.signIn.isEnabled = false
                binding.signUp.isEnabled = false
                binding.signOut.isEnabled = true
            }
            STATUS.SignedOut -> {
                binding.signIn.isEnabled = true
                binding.signUp.isEnabled = true
                binding.signOut.isEnabled = false
            }
        }
    }

    private fun emptyFields() {
        binding.emailText.setText("")
        binding.passwordText.setText("")
    }

    private fun emptyResults() {
        binding.resultAccessToken.text = ""
        binding.resultIdToken.text = ""
    }

    private fun displayAccount(accountState: AccountState) {
        CoroutineScope(Dispatchers.Main).launch {
            val accessTokenState = accountState.getAccessToken()
            if (accessTokenState is GetAccessTokenResult.Complete) {
                val accessToken = accessTokenState.resultValue.accessToken
                binding.resultAccessToken.text =
                    getString(R.string.result_access_token_text) + accessToken

                val idToken = accountState.getIdToken()
                binding.resultIdToken.text = getString(R.string.result_id_token_text) + idToken
            }
        }
    }

    private fun displayDialog(error: String? = null, message: String?) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(error)
            .setMessage(message)
        val alertDialog = builder.create()
        alertDialog.show()
    }

    /**
     * Callback used for interactive request.
     * If succeeds we use the access token to call the Microsoft Graph.
     * Does not check cache.
     */
    private fun getAuthInteractiveCallback(): AuthenticationCallback {
        return object : AuthenticationCallback {

            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                /* Successfully got a token, use it to call a protected resource - MSGraph */

                val accountResult = authenticationResult.account as Account

                /* Update account */
                emptyFields()
                updateUI(STATUS.SignedIn)
                val idToken = accountResult.idToken
                binding.resultIdToken.text =
                    getString(R.string.result_id_token_text) + idToken

                Toast.makeText(requireContext(), getString(R.string.sign_in_successful_message), Toast.LENGTH_SHORT).show()
            }

            override fun onError(exception: MsalException) {
                /* Failed to acquireToken */
                displayDialog(getString(R.string.msal_exception_title), exception.errorCode)
            }

            override fun onCancel() {
                /* User canceled the authentication */
            }
        }
    }

    private fun navigateToSignUp(
        nextState: SignUpCodeRequiredState,
        codeLength: Int,
        sentTo: String,
        channel: String
    ) {
        val bundle = Bundle()
        bundle.putParcelable(Constants.STATE, nextState)
        bundle.putInt(Constants.CODE_LENGTH, codeLength)
        bundle.putString(Constants.SENT_TO, sentTo)
        bundle.putString(Constants.CHANNEL, channel)
        val fragment = SignUpCodeFragment()
        fragment.arguments = bundle

        requireActivity().supportFragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .addToBackStack(fragment::class.java.name)
            .replace(R.id.scenario_fragment, fragment)
            .commit()
    }
}
