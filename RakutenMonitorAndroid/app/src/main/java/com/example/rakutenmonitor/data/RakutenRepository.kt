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
                        val pageTitle = view?.title ?: "Unknown"
                        onProgress("Login: ${pageTitle.take(10)}..")
                        Log.d(TAG, "Injecting generic login script...")
                        
                        val js = """
                            (function() {
                                var attempts = 0;
                                var interval = setInterval(function() {
                                    attempts++;
                                    if (attempts > 40) { // 20 seconds
                                        clearInterval(interval);
                                        return;
                                    }

                                    // GENERIC HEURISTIC: Find Password Field First
                                    var p = document.querySelector('input[type="password"]');
                                    var u = null;

                                    if (p) {
                                        // Look for username field (text/email/tel) that is NOT hidden
                                        var inputs = Array.from(document.querySelectorAll('input:not([type="hidden"]):not([type="submit"]):not([type="button"])'));
                                        var pIndex = inputs.indexOf(p);
                                        if (pIndex > 0) {
                                            u = inputs[pIndex - 1]; // Assume the field before password is username
                                        }
                                    }
                                    
                                    // Fallback to specific names if generic fails
                                    if (!u) u = document.querySelector('input[name="u"], input[name="username"], input[name="login_id"]');

                                    if (u && p) {
                                        clearInterval(interval);
                                        
                                        u.value = '$userId';
                                        p.value = '$pass';
                                        
                                        u.dispatchEvent(new Event('input', { bubbles: true }));
                                        u.dispatchEvent(new Event('change', { bubbles: true }));
                                        p.dispatchEvent(new Event('input', { bubbles: true }));
                                        p.dispatchEvent(new Event('change', { bubbles: true }));
                                        
                                        // Generic Submit Finder
                                        setTimeout(function() {
                                            // 1. Try form submit button
                                            var btn = p.form ? p.form.querySelector('button, input[type="submit"]') : null;
                                            
                                            // 2. Try generic selectors
                                            if (!btn) btn = document.querySelector('button[type="submit"], input[type="submit"], .loginButton, button[class*="submit"], button[class*="login"]');
                                            
                                            if (btn) {
                                                btn.click();
                                            } else if (p.form) {
                                                p.form.submit();
                                            } else if (document.forms.length > 0) {
                                                document.forms[0].submit();
                                            }
                                        }, 1000); 
                                    }
                                }, 500);
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
