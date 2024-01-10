package com.microsoft.identity.client.testapp.nativeauth

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.testapp.R
import com.microsoft.identity.client.testapp.databinding.FragmentCityBinding
import com.microsoft.identity.nativeauth.UserAttributes
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult
import com.microsoft.identity.nativeauth.statemachine.states.SignInAfterSignUpState
import com.microsoft.identity.nativeauth.statemachine.states.SignUpAttributesRequiredState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignUpCityAttributeFragment : Fragment() {
    private lateinit var currentState: SignUpAttributesRequiredState
    private var _binding: FragmentCityBinding? = null
    private val binding get() = _binding!!

    companion object {
        private val TAG = SignUpCityAttributeFragment::class.java.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCityBinding.inflate(inflater, container, false)

        val bundle = this.arguments
        currentState = bundle!!.getSerializable(Constants.STATE) as SignUpAttributesRequiredState

        init()

        return binding.root
    }

    private fun init() {
        initializeButtonListeners()
    }

    private fun initializeButtonListeners() {
        binding.submit.setOnClickListener {
            submit()
        }
    }

    private fun submit() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val city = binding.cityText.text.toString()

                val cityAttribute = UserAttributes.Builder
                    .country(city)
                    .build()

                val actionResult = currentState.submitAttributes(cityAttribute)

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
                        displayError("Unexpected action result: $actionResult")
                    }
                }
            } catch (exception: MsalException) {
                displayError(exception.message.toString())
            }
        }
    }

    private fun displayError(errorMsg: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.msal_exception_title))
            .setMessage(errorMsg)
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
                displayError("Unexpected action result: $actionResult")
            }
        }
    }

    private fun finish() {
        val fragmentManager = requireActivity().supportFragmentManager
        val name: String? = fragmentManager.getBackStackEntryAt(0).name
        fragmentManager.popBackStack(name, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
}