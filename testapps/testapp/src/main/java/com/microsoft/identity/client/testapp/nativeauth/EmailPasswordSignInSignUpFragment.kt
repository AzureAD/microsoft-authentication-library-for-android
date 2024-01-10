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
import com.microsoft.identity.nativeauth.statemachine.states.SignInAfterSignUpState
import com.microsoft.identity.nativeauth.statemachine.states.SignUpCodeRequiredState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EmailPasswordSignInSignUpFragment : Fragment() {

    private lateinit var authClient: INativeAuthPublicClientApplication
    private var _binding: FragmentEmailPasswordBinding? = null
    private val binding get() = _binding!!

    companion object {
        private val TAG = EmailPasswordSignInSignUpFragment::class.java.simpleName
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

                val actionResult = authClient.signInUsingPassword(
                    username = email,
                    password = password
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

                val actionResult = authClient.signUpUsingPassword(
                    username = email,
                    password = password
                )

                password.fill('0')

                when (actionResult) {
                    is SignUpResult.CodeRequired -> {
                        navigateToSignUp(
                            nextState = actionResult.nextState
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

    private suspend fun signInAfterSignUp(nextState: SignInAfterSignUpState) {
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
        Log.w(TAG, "$message")
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
                Log.d(TAG, "Successfully authenticated")

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
                Log.d(TAG, "Authentication failed: $exception")
                displayDialog(getString(R.string.msal_exception_title), exception.errorCode)
            }

            override fun onCancel() {
                /* User canceled the authentication */
                Log.d(TAG, "User cancelled login.")
            }
        }
    }
    private fun navigateToSignUp(nextState: SignUpCodeRequiredState) {
        val bundle = Bundle()
        bundle.putSerializable(Constants.STATE, nextState)
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
