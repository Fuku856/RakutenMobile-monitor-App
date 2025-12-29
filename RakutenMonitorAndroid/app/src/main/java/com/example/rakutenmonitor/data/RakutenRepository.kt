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

    suspend fun fetchData(onProgress: (String) -> Unit = {}): Result<Double> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            onProgress("Initializing WebView...")
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            
            // Sync cookies just in case, though WebView does it automatically usually
            val cookieManager = android.webkit.CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)

            // User Agent (generic modern mobile)
            webView.settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

            var checkLoopStarted = false
            var attempts = 0
            val maxAttempts = 30 
            val handler = Handler(Looper.getMainLooper())

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "Page loaded: $url")

                    // 1. Check for Login Redirect
                    val isLoginPage = url?.contains("login.account.rakuten.com") == true || 
                                      url?.contains("id.rakuten.co.jp") == true ||
                                      url?.contains("member/login") == true // generic catch

                    if (isLoginPage) {
                        Log.d(TAG, "Login page detected. Manual login required.")
                        if (continuation.isActive) {
                            continuation.resumeWithException(LoginRequiredException())
                            webView.destroy()
                        }
                        return
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
                                        return el ? el.innerText : null; // "3.2 GB" / "データ使用量" etc
                                    })();
                                """.trimIndent()

                                view?.evaluateJavascript(jsCheck) { value ->
                                    Log.d(TAG, "JS Check attempt $attempts: $value")
                                    
                                    if (value != null && value != "null" && value != "null") {
                                        // Attempt to parse number
                                        // Format might be "3.0GB" or "3.0"
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
                                            // Ensure we don't loop forever if text is weird, but for now retry
                                             handler.postDelayed(this, 1000)
                                        }
                                    } else {
                                        handler.postDelayed(this, 1000)
                                    }
                                }
                            }
                        }
                        handler.post(checkRunnable)
                    }
                }
            }

            Log.d(TAG, "Loading dashboard URL...")
            onProgress("Connecting...")
            webView.loadUrl("https://portal.mobile.rakuten.co.jp/dashboard")
        }
    }
}

class LoginRequiredException : Exception("Login required")
