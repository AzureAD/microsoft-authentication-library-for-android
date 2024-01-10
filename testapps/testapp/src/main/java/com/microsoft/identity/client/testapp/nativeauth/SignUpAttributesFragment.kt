package com.microsoft.identity.client.testapp.nativeauth

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.testapp.R
import com.microsoft.identity.client.testapp.databinding.FragmentAttributeBinding
import com.microsoft.identity.nativeauth.UserAttributes
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult
import com.microsoft.identity.nativeauth.statemachine.states.SignInAfterSignUpState
import com.microsoft.identity.nativeauth.statemachine.states.SignUpAttributesRequiredState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignUpAttributesFragment : Fragment() {
    private lateinit var currentState: SignUpAttributesRequiredState
    private var _binding: FragmentAttributeBinding? = null
    private val binding get() = _binding!!

    companion object {
        private val TAG = SignUpAttributesFragment::class.java.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAttributeBinding.inflate(inflater, container, false)

        val bundle = this.arguments
        currentState = bundle!!.getSerializable(Constants.STATE) as SignUpAttributesRequiredState

        init()

        return binding.root
    }

    private fun init() {
        initializeButtonListeners()
    }

    private fun initializeButtonListeners() {
        binding.create.setOnClickListener {
            create()
        }
    }

    private fun create() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val country = binding.countryText.text.toString()
                val city = binding.cityText.text.toString()

                val attributes = UserAttributes.Builder
                    .country(country)
                    .city(city)
                    .build()

                val actionResult = currentState.submitAttributes(attributes)

                when (actionResult) {
                    is SignUpResult.Complete -> {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.sign_up_successful_message),
                            Toast.LENGTH_SHORT
                        ).show()
                        signInAfterSignUp(actionResult.nextState)
                    }
                    else -> {
                        displayDialog(getString(R.string.msal_exception_title),"Unexpected result: $actionResult")
                    }
                }
            } catch (exception: MsalException) {
                displayDialog(getString(R.string.msal_exception_title), exception.message.toString())
            }
        }
    }
    private fun displayDialog(error: String?, message: String?) {
        Log.w(TAG, "$message")

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(error)
            .setMessage(message)
        val alertDialog = builder.create()
        alertDialog.show()
    }
    private suspend fun signInAfterSignUp(nextState: SignInAfterSignUpState) {
        val currentState = nextState
        val actionResult = currentState.signIn(null)
        when (actionResult) {
            is SignInResult.Complete -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.sign_in_successful_message),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
            else -> {
                displayDialog(getString(R.string.msal_exception_title),"Unexpected result: $actionResult")
            }
        }
    }

    private fun finish() {
        val fragmentManager = requireActivity().supportFragmentManager
        val name: String? = fragmentManager.getBackStackEntryAt(0).name
        fragmentManager.popBackStack(name, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
}