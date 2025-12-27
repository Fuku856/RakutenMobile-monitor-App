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

                    // 1. Handle Login Page (Supports both old and new login domains)
                    val isLoginPage = url?.contains("login.account.rakuten.com") == true || 
                                      url?.contains("id.rakuten.co.jp") == true
                                      
                    if (isLoginPage && !loginScriptExecuted) {
                        loginScriptExecuted = true
                        onProgress("Logging in...")
                        Log.d(TAG, "Injecting login script...")
                        
                        val js = """
                            (function() {
                                var attempts = 0;
                                var interval = setInterval(function() {
                                    attempts++;
                                    if (attempts > 40) { // 20 seconds timeout
                                        clearInterval(interval);
                                        return;
                                    }

                                    // Try various selectors for User/Pass
                                    var u = document.getElementById('loginInner_u') || 
                                            document.querySelector('input[name="u"]') || 
                                            document.querySelector('input[type="text"][name*="user"]') ||
                                            document.querySelector('input[type="email"]');
                                            
                                    var p = document.getElementById('loginInner_p') || 
                                            document.querySelector('input[name="p"]') || 
                                            document.querySelector('input[type="password"]');
                                    
                                    if (u && p) {
                                        clearInterval(interval);
                                        
                                        u.value = '$userId';
                                        p.value = '$pass';
                                        
                                        // Fire events to ensure frameworks (React/Vue) pick up the change
                                        u.dispatchEvent(new Event('input', { bubbles: true }));
                                        u.dispatchEvent(new Event('change', { bubbles: true }));
                                        p.dispatchEvent(new Event('input', { bubbles: true }));
                                        p.dispatchEvent(new Event('change', { bubbles: true }));
                                        
                                        // Submit
                                        setTimeout(function() {
                                            var btn = document.querySelector('input[type="submit"]') || 
                                                      document.querySelector('button[type="submit"]') ||
                                                      document.querySelector('.loginButton'); // Generic class guesstimate
                                                      
                                            if (btn) {
                                                btn.click();
                                            } else if (document.forms.length > 0) {
                                                document.forms[0].submit();
                                            }
                                        }, 1000); 
                                    }
                                }, 500); // Check every 500ms
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(js, null)
                    }

                    // 2. Handle Dashboard Page
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

            Log.d(TAG, "Loading initial URL...")
            onProgress("Connecting...")
            // Load dashboard directly, let it redirect to login
            webView.loadUrl("https://portal.mobile.rakuten.co.jp/dashboard")
        }
    }
}
