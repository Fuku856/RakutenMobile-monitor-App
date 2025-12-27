package com.example.rakutenmonitor.data

import android.util.Log
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException

class RakutenRepository {
    private val TAG = "RakutenRepo"
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .cookieJar(SimpleCookieJar())
        .build()

    fun fetchData(userId: String, pass: String): Result<Double> {
        return try {
            Log.d(TAG, "Starting data fetch...")
            
            // 1. Initial Access to get cookies/tokens and parse form
            val loginUrl = "https://grp03.id.rakuten.co.jp/rms/nid/login?service_id=rm001&client_id=rmn_app_web&redirect_uri=https%3A%2F%2Fportal.mobile.rakuten.co.jp%2Fdashboard&scope=memberinfo_read_safebulk%2Cmemberinfo_read_point%2Cmemberinfo_get_card_token%2C30days%40Access%2C90days%40Refresh&contact_info_required=false&rae_service_id=rm001"
            
            val initialRequest = Request.Builder().url(loginUrl).build()
            val initialResponse = client.newCall(initialRequest).execute()
            if (!initialResponse.isSuccessful) {
                Log.e(TAG, "Initial access failed: ${initialResponse.code}")
                return Result.failure(IOException("Initial access failed: ${initialResponse.code}"))
            }
            
            val initialHtml = initialResponse.body?.string() ?: ""
            initialResponse.close()

            val doc = Jsoup.parse(initialHtml)
            
            // 2. Prepare Form Body by extracting real inputs
            val formBodyBuilder = FormBody.Builder()
            val inputs = doc.select("input")
            
            var userFieldFound = false
            var passFieldFound = false
            
            for (input in inputs) {
                val name = input.attr("name")
                val type = input.attr("type")
                val value = input.attr("value")
                
                if (name.isEmpty()) continue
                
                when {
                    type == "hidden" -> formBodyBuilder.add(name, value)
                    type == "password" -> {
                        formBodyBuilder.add(name, pass)
                        passFieldFound = true
                    }
                    type == "text" || type == "email" || type == "tel" -> {
                        // Assumption: First text-like field is username
                        if (!userFieldFound && !name.contains("search", ignoreCase = true)) {
                            formBodyBuilder.add(name, userId)
                            userFieldFound = true
                        } else {
                            formBodyBuilder.add(name, value)
                        }
                    }
                    type == "submit" || type == "image" -> {
                        // Include submit buttons
                        formBodyBuilder.add(name, value)
                    }
                }
            }
            
            // Fallbacks if extraction failed (unlikely if page loaded correctly)
            if (!userFieldFound) formBodyBuilder.add("u", userId)
            if (!passFieldFound) formBodyBuilder.add("p", pass)

            Log.d(TAG, "Submitting login form...")

            // 3. POST Login
            val loginRequest = Request.Builder()
                .url(loginUrl)
                .post(formBodyBuilder.build())
                .build()

            val loginResponse = client.newCall(loginRequest).execute()
            if (!loginResponse.isSuccessful) {
                 Log.e(TAG, "Login POST failed: ${loginResponse.code}")
                 return Result.failure(IOException("Login POST failed: ${loginResponse.code}"))
            }
            
            // Consume body to ensure connection reuse / cookie processing
            loginResponse.body?.string()
            loginResponse.close()

            // 4. Fetch Dashboard
            Log.d(TAG, "Fetching dashboard...")
            val dashboardUrl = "https://portal.mobile.rakuten.co.jp/dashboard"
            val dashboardRequest = Request.Builder().url(dashboardUrl).build()
            val dashboardResponse = client.newCall(dashboardRequest).execute()
            
            if (!dashboardResponse.isSuccessful) {
                Log.e(TAG, "Dashboard access failed: ${dashboardResponse.code}")
                return Result.failure(IOException("Dashboard access failed: ${dashboardResponse.code}"))
            }
            
            val html = dashboardResponse.body?.string() ?: ""
            dashboardResponse.close()

            // 5. Parse Data
            val dashboardDoc = Jsoup.parse(html)
            val usageElement = dashboardDoc.select("div.title__data").first()
            
            if (usageElement != null) {
                // "19.5 GB" -> 19.5
                val text = usageElement.text().replace("GB", "", ignoreCase = true).replace(" ", "")
                val usage = text.toDoubleOrNull()
                
                if (usage != null) {
                    Log.d(TAG, "Success! Usage: $usage")
                    Result.success(usage)
                } else {
                    Log.e(TAG, "Parse error: $text")
                    Result.failure(IOException("Parse error: $text"))
                }
            } else {
                Log.e(TAG, "Usage element not found. Title: ${dashboardDoc.title()}")
                 // Check if we are still on login page
                if (dashboardDoc.title().contains("Login", ignoreCase = true) || 
                    dashboardDoc.select("input[type=password]").isNotEmpty()) {
                    Result.failure(IOException("Login failed (still on login page)"))
                } else {
                    Result.failure(IOException("Usage element not found"))   
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
