////   Copyright (c) Microsoft Corporation.
////   All rights reserved.
////
////   This code is licensed under the MIT License.
////
////   Permission is hereby granted, free of charge, to any person obtaining a copy
////   of this software and associated documentation files(the "Software"), to deal
////   in the Software without restriction, including without limitation the rights
////   to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
////   copies of the Software, and to permit persons to whom the Software is
////   furnished to do so, subject to the following conditions :
////
////   The above copyright notice and this permission notice shall be included in
////   all copies or substantial portions of the Software.
////
////   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
////   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
////   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
////   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
////   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
////   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
////   THE SOFTWARE.
//package com.microsoft.identity.client.testapp
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.appcompat.app.AppCompatActivity
//import androidx.fragment.app.Fragment
//import com.google.android.material.bottomnavigation.BottomNavigationView
//import com.microsoft.identity.client.testapp.nativeauth.AuthClient
//import com.microsoft.identity.client.testapp.nativeauth.EmailAttributeSignUpFragment
//import com.microsoft.identity.client.testapp.nativeauth.EmailPasswordSignInSignUpFragment
//import com.microsoft.identity.client.testapp.nativeauth.EmailSignInSignUpFragment
//import com.microsoft.identity.client.testapp.nativeauth.PasswordResetFragment
//
///**
// * Fragment used for starting various native auth flows.
// */
//class NativeAuthFragment : Fragment() {
//    companion object {
//        private val TAG = NativeAuthFragment::class.java.simpleName
//    }
//
//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
//        val view = inflater.inflate(R.layout.fragment_native, container, false)
//
//        AuthClient.initialize(requireContext())
//
//        val emailSignInSignUpFragment = EmailSignInSignUpFragment()
//        val emailPasswordSignInSignUpFragment = EmailPasswordSignInSignUpFragment()
//        val emailAttributeSignUpFragment = EmailAttributeSignUpFragment()
//        val passwordResetFragment = PasswordResetFragment()
//
//        val bottomNavigationView = view.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
//
//        setFragment(emailSignInSignUpFragment, R.string.title_email_oob_sisu)
//
//        bottomNavigationView.setOnNavigationItemSelectedListener {
//            when (it.itemId) {
//                R.id.email_oob_sisu -> setFragment(emailSignInSignUpFragment, R.string.title_email_oob_sisu)
//                R.id.email_password_sisu -> setFragment(emailPasswordSignInSignUpFragment, R.string.title_email_password_sisu)
//                R.id.email_attribute_sisu -> setFragment(emailAttributeSignUpFragment, R.string.title_email_attribute_oob_sisu)
//                R.id.email_oob_sspr -> setFragment(passwordResetFragment, R.string.title_email_oob_sspr)
//            }
//            true
//        }
//
//        return view
//    }
//
//    private fun setFragment(fragment: Fragment, title: Int) {
//        (this.context as AppCompatActivity).supportFragmentManager.beginTransaction().apply {
//            replace(R.id.scenario_fragment, fragment)
//            commit()
//        }
//    }
//}