package com.microsoft.identity.client.testapp.nativeauth

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.testapp.R
import com.microsoft.identity.client.testapp.databinding.FragmentCodeBinding
import com.microsoft.identity.nativeauth.statemachine.results.SignInResendCodeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.statemachine.states.SignInCodeRequiredState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignInCodeFragment : Fragment() {
    private lateinit var currentState: SignInCodeRequiredState
    private var _binding: FragmentCodeBinding? = null
    private val binding get() = _binding!!

    companion object {
        private val TAG = SignInCodeFragment::class.java.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCodeBinding.inflate(inflater, container, false)

        val bundle = this.arguments
        currentState = bundle!!.getSerializable(Constants.STATE) as SignInCodeRequiredState

        init()

        return binding.root
    }

    private fun init() {
        initializeButtonListeners()
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
                val emailCode = binding.codeText.text.toString()

                val actionResult = currentState.submitCode(emailCode)

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
                is SignInResendCodeResult.Success -> {
                    currentState = actionResult.nextState
                    Toast.makeText(requireContext(), getString(R.string.resend_code_message), Toast.LENGTH_LONG).show()
                }
                else -> {
                    displayDialog(getString(R.string.msal_exception_title),"Unexpected result: $actionResult")
                }
            }
        }
    }

    private fun clearCode() {
        binding.codeText.text?.clear()
    }
    private fun displayDialog(error: String?, message: String?) {
        Log.w(TAG, "$message")

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(error)
            .setMessage(message)
        val alertDialog = builder.create()
        alertDialog.show()
    }
    private fun finish() {
        requireActivity().supportFragmentManager.popBackStackImmediate()
    }
}
