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
import com.microsoft.identity.client.testapp.R
import com.microsoft.identity.client.testapp.databinding.FragmentCodeBinding
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordResendCodeResult
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordSubmitCodeResult
import com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordCodeRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordPasswordRequiredState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PasswordResetCodeFragment : Fragment() {
    private lateinit var currentState: ResetPasswordCodeRequiredState
    private var _binding: FragmentCodeBinding? = null
    private val binding get() = _binding!!

    companion object {
        private val TAG = PasswordResetCodeFragment::class.java.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCodeBinding.inflate(inflater, container, false)
        val view = binding.root

        val bundle = this.arguments
        currentState = bundle!!.getSerializable(Constants.STATE) as ResetPasswordCodeRequiredState

        init()

        return view
    }

    private fun init() {
        initializeButtonListeners()
    }

    private fun initializeButtonListeners() {
        binding.verifyCode.setOnClickListener {
            submitCode()
        }

        binding.resendCodeText.setOnClickListener {
            resendCode()
        }
    }

    private fun submitCode() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val code = binding.codeText.text.toString()

                val actionResult = currentState.submitCode(code)

                when (actionResult) {
                    is ResetPasswordSubmitCodeResult.PasswordRequired -> {
                        navigateToResetPasswordPasswordFragment(
                            nextState = actionResult.nextState
                        )
                    }
                    else -> {
                        displayDialog(getString(R.string.msal_exception_title), "Unexpected result: $actionResult")
                    }
                }
            } catch (exception: MsalException) {
                displayDialog(getString(R.string.msal_exception_title), exception.message.toString())
            }
        }
    }

    private fun resendCode() {
        clearCode()

        CoroutineScope(Dispatchers.Main).launch {
            val actionResult = currentState.resendCode()

            when (actionResult) {
                is ResetPasswordResendCodeResult.Success -> {
                    currentState = actionResult.nextState
                    Toast.makeText(requireContext(), getString(R.string.resend_code_message), Toast.LENGTH_LONG).show()
                }
                else -> {
                    displayDialog(getString(R.string.msal_exception_title), "Unexpected result: $actionResult")
                }
            }
        }
    }

    private fun clearCode() {
        binding.codeText.text?.clear()
    }

    private fun displayDialog(error: String?, message: String?) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(error)
            .setMessage(message)
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun navigateToResetPasswordPasswordFragment(nextState: ResetPasswordPasswordRequiredState) {
        val bundle = Bundle()
        bundle.putSerializable(Constants.STATE, nextState)
        val fragment = PasswordResetNewPasswordFragment()
        fragment.arguments = bundle

        requireActivity().supportFragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .addToBackStack(fragment::class.java.name)
            .replace(R.id.scenario_fragment, fragment)
            .commit()
    }
}
