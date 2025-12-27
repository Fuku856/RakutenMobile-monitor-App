package com.example.rakutenmonitor.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class RakutenRepository(private val context: Context) {
    private val TAG = "RakutenRepo"

    suspend fun fetchData(userId: String, pass: String, onProgress: (String) -> Unit = {}): Result<Double> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            onProgress("Initializing WebView...")
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

            var loginScriptExecuted = false
            var checkLoopStarted = false
            var attempts = 0
            val maxAttempts = 30 

            val handler = Handler(Looper.getMainLooper())

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "Page loaded: $url")

                    if (url?.contains("id.rakuten.co.jp/rms/nid/login") == true && !loginScriptExecuted) {
                        loginScriptExecuted = true
                        onProgress("Logging in...")
                        Log.d(TAG, "Injecting login script...")
                        
                        val js = """
                            (function() {
                                var u = document.querySelector('input[name="u"]') || document.querySelector('input[type="text"]');
                                var p = document.querySelector('input[name="p"]') || document.querySelector('input[type="password"]');
                                var buttons = document.querySelectorAll('input[type="submit"], button[type="submit"]');
                                
                                if (u && p) {
                                    u.value = '$userId';
                                    p.value = '$pass';
                                    u.dispatchEvent(new Event('change'));
                                    p.dispatchEvent(new Event('change'));
                                    
                                    setTimeout(function() {
                                        if (buttons.length > 0) {
                                            buttons[0].click();
                                        } else {
                                            document.forms[0].submit();
                                        }
                                    }, 500);
                                }
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(js, null)
                    }

                    if (url?.contains("portal.mobile.rakuten.co.jp/dashboard") == true && !checkLoopStarted) {
                        checkLoopStarted = true
                        onProgress("Dashboard access...")
                        Log.d(TAG, "Dashboard loaded. Starting data check loop...")
                        
                        val checkRunnable = object : Runnable {
                            override fun run() {
                                if (attempts >= maxAttempts) {
                                    if (continuation.isActive) {
                                        onProgress("Timeout!")
                                        continuation.resume(Result.failure(Exception("Timeout waiting for data element")))
                                        webView.destroy()
                                    }
                                    return
                                }
                                attempts++
                                onProgress("Reading data... ($attempts/$maxAttempts)")

                                val jsCheck = """
                                    (function() {
                                        var el = document.querySelector('div.title__data');
                                        return el ? el.innerText : null;
                                    })();
                                """.trimIndent()

                                view?.evaluateJavascript(jsCheck) { value ->
                                    Log.d(TAG, "JS Check attempt $attempts: $value")
                                    
                                    if (value != null && value != "null" && value != "null") {
                                        val cleanValue = value.replace("\"", "")
                                            .replace("GB", "", ignoreCase = true)
                                            .replace(" ", "")
                                        
                                        val doubleVal = cleanValue.toDoubleOrNull()
                                        
                                        if (doubleVal != null) {
                                            Log.d(TAG, "Data found: $doubleVal")
                                            if (continuation.isActive) {
                                                onProgress("Success!")
                                                continuation.resume(Result.success(doubleVal))
                                                webView.destroy()
                                            }
                                        } else {
                                            handler.postDelayed(this, 2000)
                                        }
                                    } else {
                                        handler.postDelayed(this, 2000)
                                    }
                                }
                            }
                        }
                        handler.post(checkRunnable)
                    }
                }
            }

            Log.d(TAG, "Loading initial login URL...")
            onProgress("Connecting...")
             val loginUrl = "https://grp03.id.rakuten.co.jp/rms/nid/login?service_id=rm001&client_id=rmn_app_web&redirect_uri=https%3A%2F%2Fportal.mobile.rakuten.co.jp%2Fdashboard&scope=memberinfo_read_safebulk%2Cmemberinfo_read_point%2Cmemberinfo_get_card_token%2C30days%40Access%2C90days%40Refresh&contact_info_required=false&rae_service_id=rm001"
            webView.loadUrl(loginUrl)
        }
    }
}
