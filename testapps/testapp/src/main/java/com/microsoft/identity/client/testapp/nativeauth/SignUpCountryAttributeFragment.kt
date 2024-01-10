package com.microsoft.identity.client.testapp.nativeauth

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.testapp.R
import com.microsoft.identity.client.testapp.databinding.FragmentCountryBinding
import com.microsoft.identity.nativeauth.UserAttributes
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult
import com.microsoft.identity.nativeauth.statemachine.states.SignUpAttributesRequiredState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignUpCountryAttributeFragment : Fragment() {
    private lateinit var currentState: SignUpAttributesRequiredState
    private var _binding: FragmentCountryBinding? = null
    private val binding get() = _binding!!

    companion object {
        private val TAG = SignUpCountryAttributeFragment::class.java.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCountryBinding.inflate(inflater, container, false)

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
                val country = binding.countryText.text.toString()

                val countryAttribute = UserAttributes.Builder
                    .country(country)
                    .build()

                val actionResult = currentState.submitAttributes(countryAttribute)

                when (actionResult) {
                    is SignUpResult.AttributesRequired -> {
                        navigateToCity(actionResult.nextState)
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

    private fun displayError(errorMsg: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.msal_exception_title))
            .setMessage(errorMsg)
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun navigateToCity(nextState: SignUpAttributesRequiredState) {
        val bundle = Bundle()
        bundle.putSerializable(Constants.STATE, nextState)
        val fragment = SignUpCityAttributeFragment()
        fragment.arguments = bundle

        requireActivity().supportFragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .addToBackStack(fragment::class.java.name)
            .replace(R.id.scenario_fragment, fragment)
            .commit()
    }
}