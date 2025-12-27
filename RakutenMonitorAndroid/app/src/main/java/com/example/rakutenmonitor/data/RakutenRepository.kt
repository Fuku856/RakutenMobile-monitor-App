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

    suspend fun fetchData(userId: String, pass: String): Result<Double> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            // Use a standard mobile User-Agent
            webView.settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

            var loginScriptExecuted = false
            var checkLoopStarted = false
            var attempts = 0
            val maxAttempts = 30 // 30 * 2sec = 60sec timeout

            val handler = Handler(Looper.getMainLooper())

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "Page loaded: $url")

                    // 1. Handle Login Page
                    if (url?.contains("id.rakuten.co.jp/rms/nid/login") == true && !loginScriptExecuted) {
                        loginScriptExecuted = true
                        Log.d(TAG, "Injecting login script...")
                        
                        // JS to fill form and submit. 
                        // Note: Using specific input names 'u' and 'p' often found in Rakuten login, 
                        // fallback to generic types if names differ.
                        val js = """
                            (function() {
                                var u = document.querySelector('input[name="u"]') || document.querySelector('input[type="text"]');
                                var p = document.querySelector('input[name="p"]') || document.querySelector('input[type="password"]');
                                var buttons = document.querySelectorAll('input[type="submit"], button[type="submit"]');
                                
                                if (u && p) {
                                    u.value = '$userId';
                                    p.value = '$pass';
                                    
                                    // Trigger change events just in case
                                    u.dispatchEvent(new Event('change'));
                                    p.dispatchEvent(new Event('change'));
                                    
                                    // Try updating 'u' and 'p' fields if they exist as hidden/renamed logic is tricky
                                    // For now, simple submit
                                    
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

                    // 2. Handle Dashboard Page
                    if (url?.contains("portal.mobile.rakuten.co.jp/dashboard") == true && !checkLoopStarted) {
                        checkLoopStarted = true
                        Log.d(TAG, "Dashboard loaded. Starting data check loop...")
                        
                        val checkRunnable = object : Runnable {
                            override fun run() {
                                // Timeout check
                                if (attempts >= maxAttempts) {
                                    if (continuation.isActive) {
                                        continuation.resume(Result.failure(Exception("Timeout waiting for data element")))
                                        webView.destroy()
                                    }
                                    return
                                }
                                attempts++

                                // Check for element
                                val jsCheck = """
                                    (function() {
                                        var el = document.querySelector('div.title__data');
                                        return el ? el.innerText : null;
                                    })();
                                """.trimIndent()

                                view?.evaluateJavascript(jsCheck) { value ->
                                    // value is returned as a JSON string, e.g. "\"19.5 GB\"" or "null"
                                    Log.d(TAG, "JS Check attempt $attempts: $value")
                                    
                                    if (value != null && value != "null" && value != "null") {
                                        // Clean up value
                                        val cleanValue = value.replace("\"", "")
                                            .replace("GB", "", ignoreCase = true)
                                            .replace(" ", "")
                                        
                                        val doubleVal = cleanValue.toDoubleOrNull()
                                        
                                        if (doubleVal != null) {
                                            Log.d(TAG, "Data found: $doubleVal")
                                            if (continuation.isActive) {
                                                continuation.resume(Result.success(doubleVal))
                                                webView.destroy() // Cleanup
                                            }
                                        } else {
                                            // Found element text but couldn't parse number? 
                                            // Might be loading "..." or similar. Retry.
                                            handler.postDelayed(this, 2000)
                                        }
                                    } else {
                                        // Element not found yet. Retry.
                                        handler.postDelayed(this, 2000)
                                    }
                                }
                            }
                        }
                        // Start polling
                        handler.post(checkRunnable)
                    }
                }
            }

            Log.d(TAG, "Loading initial login URL...")
            // Standard login URL with redirect to dashboard
             val loginUrl = "https://grp03.id.rakuten.co.jp/rms/nid/login?service_id=rm001&client_id=rmn_app_web&redirect_uri=https%3A%2F%2Fportal.mobile.rakuten.co.jp%2Fdashboard&scope=memberinfo_read_safebulk%2Cmemberinfo_read_point%2Cmemberinfo_get_card_token%2C30days%40Access%2C90days%40Refresh&contact_info_required=false&rae_service_id=rm001"
            webView.loadUrl(loginUrl)
        }
    }
}
