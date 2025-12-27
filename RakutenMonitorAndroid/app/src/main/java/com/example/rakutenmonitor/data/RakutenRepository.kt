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
                    // 1. Handle Login Page (Supports both old and new login domains)
                    val isLoginPage = url?.contains("login.account.rakuten.com") == true || 
                                      url?.contains("id.rakuten.co.jp") == true
                                      
                    if (isLoginPage) {
                        val pageTitle = view?.title ?: "Unknown"
                        onProgress("Login Step: ${pageTitle.take(10)}..")
                        Log.d(TAG, "Injecting login/consent script...")
                        
                        val js = """
                            (function() {
                                var attempts = 0;
                                
                                function findSpecificButton() {
                                    var btn = document.querySelector('input[type="submit"], button[type="submit"], .loginButton');
                                    if (!btn) {
                                        var buttons = Array.from(document.querySelectorAll('button, a.btn, input[type="button"]'));
                                        btn = buttons.find(b => {
                                            var t = (b.innerText || b.value || "").toLowerCase();
                                            return t.includes('login') || t.includes('next') || t.includes('agree') || t.includes('allow') || 
                                                   t.includes('ログイン') || t.includes('次へ') || t.includes('同意');
                                        });
                                    }
                                    return btn;
                                }

                                var interval = setInterval(function() {
                                    attempts++;
                                    if (attempts > 40) { // 20 seconds timeout
                                        clearInterval(interval);
                                        return;
                                    }

                                    // 1. Find Password
                                    var p = document.querySelector('input[type="password"]');
                                    
                                    // 2. Find Username
                                    var u = null;
                                    if (p) {
                                        var inputs = Array.from(document.querySelectorAll('input:not([type="hidden"]):not([type="submit"]):not([type="button"])'));
                                        var pIndex = inputs.indexOf(p);
                                        if (pIndex > 0) u = inputs[pIndex - 1];
                                    }
                                    if (!u) u = document.querySelector('input[name="u"], input[name="username"], input[name="login_id"], input[type="email"], input[type="tel"]');

                                    // 3. Determine Action
                                    var shouldSubmit = false;
                                    var btn = findSpecificButton(); // Look for button every tick

                                    if (u || p) {
                                        // Case A/B/C: Inputs found (Login Page)
                                        if (u) {
                                            u.value = '$userId';
                                            fireEvents(u);
                                        }
                                        if (p) {
                                            p.value = '$pass';
                                            fireEvents(p);
                                        }
                                        shouldSubmit = true;
                                    } else if (btn) {
                                        // Case D: No inputs, but BUTTON found (Consent Page)
                                        // Only proceed if we actually see the button
                                        shouldSubmit = true;
                                    }
                                    // If neither inputs nor button found, Continue Loop (wait for load)

                                    if (shouldSubmit) {
                                        clearInterval(interval); // Found our target
                                        
                                        setTimeout(function() {
                                            // Re-find button to be safe
                                            var finalBtn = findSpecificButton();
                                            
                                            if (finalBtn) {
                                                console.log("Clicking button: " + (finalBtn.innerText || finalBtn.value));
                                                finalBtn.click();
                                            } else if (document.forms.length > 0) {
                                                document.forms[0].submit();
                                            }
                                        }, 500);
                                    }

                                    function fireEvents(el) {
                                        el.dispatchEvent(new Event('input', { bubbles: true }));
                                        el.dispatchEvent(new Event('change', { bubbles: true }));
                                        el.dispatchEvent(new Event('blur', { bubbles: true }));
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
