package com.microsoft.identity.client.testapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.microsoft.identity.client.testapp.nativeauth.AuthClient
import com.microsoft.identity.client.testapp.nativeauth.EmailAttributeSignUpFragment
import com.microsoft.identity.client.testapp.nativeauth.EmailPasswordSignInSignUpFragment
import com.microsoft.identity.client.testapp.nativeauth.EmailSignInSignUpFragment
import com.microsoft.identity.client.testapp.nativeauth.PasswordResetFragment

class NativeAuthFragment : Fragment() {
    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_native, container, false)

        AuthClient.initialize(requireContext())

        val emailSignInSignUpFragment = EmailSignInSignUpFragment()
        val emailPasswordSignInSignUpFragment = EmailPasswordSignInSignUpFragment()
        val emailAttributeSignUpFragment = EmailAttributeSignUpFragment()
        val passwordResetFragment = PasswordResetFragment()

        val bottomNavigationView = view.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)

        setFragment(emailSignInSignUpFragment, R.string.title_email_oob_sisu)

        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.email_oob_sisu -> setFragment(emailSignInSignUpFragment, R.string.title_email_oob_sisu)
                R.id.email_password_sisu -> setFragment(emailPasswordSignInSignUpFragment, R.string.title_email_password_sisu)
                R.id.email_attribute_sisu -> setFragment(emailAttributeSignUpFragment, R.string.title_email_attribute_oob_sisu)
                R.id.email_oob_sspr -> setFragment(passwordResetFragment, R.string.title_email_oob_sspr)
            }
            true
        }

        return view
    }

    private fun setFragment(fragment: Fragment, title: Int) {
        (this.context as AppCompatActivity).supportFragmentManager.beginTransaction().apply {
            replace(R.id.scenario_fragment, fragment)
            commit()
        }
    }
}