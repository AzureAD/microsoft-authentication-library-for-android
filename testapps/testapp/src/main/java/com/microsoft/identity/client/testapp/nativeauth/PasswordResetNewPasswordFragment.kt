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
import com.microsoft.identity.client.testapp.databinding.FragmentPasswordBinding
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordResult
import com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordPasswordRequiredState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PasswordResetNewPasswordFragment : Fragment() {
    private lateinit var currentState: ResetPasswordPasswordRequiredState
    private var _binding: FragmentPasswordBinding? = null
    private val binding get() = _binding!!

    companion object {
        private val TAG = PasswordResetNewPasswordFragment::class.java.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPasswordBinding.inflate(inflater, container, false)
        val view = binding.root

        val bundle = this.arguments
        currentState = bundle!!.getSerializable(Constants.STATE) as ResetPasswordPasswordRequiredState

        init()

        return view
    }

    private fun init() {
        initializeButtonListener()
    }

    private fun initializeButtonListener() {
        binding.submitPassword.setOnClickListener {
            resetPassword()
        }
    }

    private fun resetPassword() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val password = CharArray(binding.passwordText.length());
                binding.passwordText.text?.getChars(0, binding.passwordText.length(), password, 0);

                val actionResult = currentState.submitPassword(password)

                password.fill('0')

                when (actionResult) {
                    is ResetPasswordResult.Complete -> {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.password_reset_success_message),
                            Toast.LENGTH_LONG
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

    private fun displayDialog(error: String?, message: String?) {
        Log.w(TAG, "$message")

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(error)
            .setMessage(message)
        val alertDialog = builder.create()
        alertDialog.show()
    }
    private fun finish() {
        val fragmentManager = requireActivity().supportFragmentManager
        val name: String? = fragmentManager.getBackStackEntryAt(0).name
        fragmentManager.popBackStack(name, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
}
