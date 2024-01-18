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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.testapp.Constants
import com.microsoft.identity.client.testapp.R
import com.microsoft.identity.client.testapp.databinding.FragmentCodeBinding
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitCodeError
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResendCodeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult
import com.microsoft.identity.nativeauth.statemachine.states.SignInAfterSignUpState
import com.microsoft.identity.nativeauth.statemachine.states.SignUpAttributesRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.SignUpCodeRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.SignUpPasswordRequiredState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fragment used for managing the code step in the sign up flow.
 */
class SignUpCodeFragment : Fragment() {
    private lateinit var currentState: SignUpCodeRequiredState
    private var codeLength: Int? = null
    private var sentTo: String? = null
    private var channel: String? = null
    private var _binding: FragmentCodeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCodeBinding.inflate(inflater, container, false)

        val bundle = this.arguments
        currentState = bundle!!.getSerializable(Constants.STATE) as SignUpCodeRequiredState
        codeLength = bundle.getInt(Constants.CODE_LENGTH)
        sentTo = bundle.getString(Constants.SENT_TO)
        channel = bundle.getString(Constants.CHANNEL)

        init()

        return binding.root
    }

    private fun init() {
        initializeButtonListeners()
        binding.codeHint.text = "Code sent to ${sentTo}, by ${channel}, with length ${codeLength}}"
    }

    private fun initializeButtonListeners() {
        binding.verifyCode.setOnClickListener {
            verifyCode()
        }

        binding.resendCodeText.setOnClickListener {
            resendCode()
        }
    }

    private fun verifyCode() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val oobCode = binding.codeText.text.toString()

                val actionResult = currentState.submitCode(oobCode)

                when (actionResult) {
                    is SignUpResult.Complete -> {
                        Toast.makeText(requireContext(), getString(R.string.sign_up_successful_message), Toast.LENGTH_SHORT).show()
                        signInAfterSignUp(
                            nextState = actionResult.nextState
                        )
                    }
                    is SubmitCodeError -> {
                        if (actionResult.isInvalidCode()) {
                            Toast.makeText(
                                requireContext(),
                                actionResult.errorMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                            clearCode()
                        } else {
                            displayError("Unexpected result: $actionResult")
                        }
                    }
                    is SignUpResult.AttributesRequired -> {
                        navigateToAttributes(
                            nextState = actionResult.nextState
                        )
                    }
                    is SignUpResult.PasswordRequired -> {
                        navigateToPassword(
                            nextState = actionResult.nextState
                        )
                    }
                    else -> {
                        displayError("Unexpected result: $actionResult")
                    }
                }
            } catch (exception: MsalException) {
                displayError(exception.message.toString())
            }
        }
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
                displayError("Unexpected result: $actionResult")
            }
        }
    }

    private fun resendCode() {
        clearCode()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val actionResult = currentState.resendCode()

                when (actionResult) {
                    is SignUpResendCodeResult.Success -> {
                        currentState = actionResult.nextState
                        Toast.makeText(requireContext(), getString(R.string.resend_code_message), Toast.LENGTH_LONG).show()
                    }
                   else -> {
                        displayError("Unexpected result: $actionResult")
                   }
                }
            } catch (exception: MsalException) {
                displayError(exception.message.toString())
            }
        }
    }

    private fun clearCode() {
        binding.codeText.text?.clear()
    }

    private fun displayError(errorMsg: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.msal_exception_title))
            .setMessage(errorMsg)
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun finish() {
        requireActivity().supportFragmentManager.popBackStackImmediate()
    }

    private fun navigateToAttributes(nextState: SignUpAttributesRequiredState) {
        val bundle = Bundle()
        bundle.putSerializable(Constants.STATE, nextState)
        val fragment = SignUpAttributesFragment()
        fragment.arguments = bundle

        requireActivity().supportFragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .addToBackStack(fragment::class.java.name)
            .replace(R.id.scenario_fragment, fragment)
            .commit()
    }

    private fun navigateToPassword(nextState: SignUpPasswordRequiredState) {
        val bundle = Bundle()
        bundle.putSerializable(Constants.STATE, nextState)
        val fragment = SignUpPasswordFragment()
        fragment.arguments = bundle

        requireActivity().supportFragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .addToBackStack(fragment::class.java.name)
            .replace(R.id.scenario_fragment, fragment)
            .commit()
    }
}
