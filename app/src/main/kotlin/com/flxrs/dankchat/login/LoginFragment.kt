package com.flxrs.dankchat.login

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.flxrs.dankchat.MainFragment
import com.flxrs.dankchat.R
import com.flxrs.dankchat.databinding.LoginFragmentBinding
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.service.api.TwitchApi
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.*

class LoginFragment : Fragment() {

    lateinit var binding: LoginFragmentBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = LoginFragmentBinding.inflate(inflater, container, false)
        binding.webview.apply {
            with(settings) {
                javaScriptEnabled = true
                setSupportZoom(true)
            }
            CookieManager.getInstance().removeAllCookies(null)
            clearCache(true)
            clearFormData()
            webViewClient = TwitchAuthClient()
            loadUrl(TwitchApi.LOGIN_URL)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        (requireActivity() as AppCompatActivity).apply {
            setSupportActionBar(binding.loginToolbar)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = getString(R.string.login_title)
            }
        }
    }

    private inner class TwitchAuthClient : WebViewClient() {
        @SuppressWarnings("DEPRECATION")
        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {

            Snackbar.make(binding.root, "Error $errorCode: $description", Snackbar.LENGTH_LONG)
            Log.e(TAG, "Error $errorCode in WebView: $description")
        }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            val message = error?.description ?: return
            val code = error.errorCode
            Snackbar.make(binding.root, "Error $code: $message", Snackbar.LENGTH_LONG)
            Log.e(TAG, "Error $code in WebView: $message")
//            with(findNavController()) {
//                previousBackStackEntry?.savedStateHandle?.set(MainFragment.LOGIN_REQUEST_KEY, false)
//                navigateUp()
//            }
        }

        @SuppressWarnings("DEPRECATION")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            val urlString = url ?: ""
            val fragment = Uri.parse(urlString).fragment ?: ""
            parseOAuthToken(fragment)
            return false
        }

        @RequiresApi(Build.VERSION_CODES.N)
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val fragment = request?.url?.fragment ?: ""
            parseOAuthToken(fragment)
            return false
        }

        private fun parseOAuthToken(fragment: String) {
            if (fragment.startsWith("access_token=")) {
                val token = fragment.substringAfter("access_token=").substringBefore("&scope=")
                lifecycleScope.launch {
                    val result = TwitchApi.validateUser(token)?.let {
                        if (it.login.isNotBlank()) {
                            DankChatPreferenceStore(requireContext()).apply {
                                setOAuthKey("oauth:$token")
                                setUserName(it.login.toLowerCase(Locale.getDefault()))
                                setUserId(it.userId)
                            }
                            true
                        } else false
                    } ?: false
                    with(findNavController()) {
                        previousBackStackEntry?.savedStateHandle?.set(MainFragment.LOGIN_REQUEST_KEY, result)
                        navigateUp()
                    }
                }
            }
        }
    }

    companion object {
        private val TAG = LoginFragment::class.java.simpleName
    }
}