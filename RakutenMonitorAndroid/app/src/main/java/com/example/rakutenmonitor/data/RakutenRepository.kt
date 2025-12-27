package com.example.rakutenmonitor.data

import android.util.Log
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.util.regex.Pattern

class RakutenRepository {
    private val TAG = "RakutenRepo"
    private val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .cookieJar(SimpleCookieJar())
        .build()

    fun fetchData(userId: String, pass: String): Result<Double> {
        return try {
            Log.d(TAG, "Starting data fetch...")
            
            // 1. Initial Access
            val loginUrl = "https://grp03.id.rakuten.co.jp/rms/nid/login?service_id=rm001&client_id=rmn_app_web&redirect_uri=https%3A%2F%2Fportal.mobile.rakuten.co.jp%2Fdashboard&scope=memberinfo_read_safebulk%2Cmemberinfo_read_point%2Cmemberinfo_get_card_token%2C30days%40Access%2C90days%40Refresh&contact_info_required=false&rae_service_id=rm001"
            
            val initialRequest = Request.Builder()
                .url(loginUrl)
                .header("User-Agent", USER_AGENT)
                .build()
            val initialResponse = client.newCall(initialRequest).execute()
            if (!initialResponse.isSuccessful) {
                return Result.failure(IOException("Initial access failed: ${initialResponse.code}"))
            }
            
            val initialHtml = initialResponse.body?.string() ?: ""
            initialResponse.close()

            val doc = Jsoup.parse(initialHtml)
            
            // 2. Prepare Form
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
                        if (!userFieldFound && !name.contains("search", ignoreCase = true)) {
                            formBodyBuilder.add(name, userId)
                            userFieldFound = true
                        } else {
                            formBodyBuilder.add(name, value)
                        }
                    }
                    type == "submit" || type == "image" -> formBodyBuilder.add(name, value)
                }
            }
            
            if (!userFieldFound) formBodyBuilder.add("u", userId)
            if (!passFieldFound) formBodyBuilder.add("p", pass)

            // 3. POST Login
            val loginRequest = Request.Builder()
                .url(loginUrl)
                .header("User-Agent", USER_AGENT)
                .post(formBodyBuilder.build())
                .build()

            val loginResponse = client.newCall(loginRequest).execute()
            if (!loginResponse.isSuccessful) {
                 return Result.failure(IOException("Login POST failed: ${loginResponse.code}"))
            }
            
            loginResponse.body?.string() // Consume
            loginResponse.close()

            // 4. Fetch Dashboard
            Log.d(TAG, "Fetching dashboard...")
            val dashboardUrl = "https://portal.mobile.rakuten.co.jp/dashboard"
            val dashboardRequest = Request.Builder()
                .url(dashboardUrl)
                .header("User-Agent", USER_AGENT)
                .build()
            val dashboardResponse = client.newCall(dashboardRequest).execute()
            
            if (!dashboardResponse.isSuccessful) {
                return Result.failure(IOException("Dashboard access failed: ${dashboardResponse.code}"))
            }
            
            val html = dashboardResponse.body?.string() ?: ""
            dashboardResponse.close()

            // 5. Parse Data (Jsoup + Regex Fallback)
            val dashboardDoc = Jsoup.parse(html)
            val usageElement = dashboardDoc.select("div.title__data").first()
            
            var finalUsage: Double? = null

            if (usageElement != null) {
                val text = usageElement.text().replace("GB", "", ignoreCase = true).replace(" ", "")
                finalUsage = text.toDoubleOrNull()
            }
            
            // Fallback: Regex Search in HTML
            if (finalUsage == null) {
                Log.d(TAG, "Jsoup failed, trying regex...")
                val pattern = Pattern.compile("([0-9]+\\.[0-9]+)\\s*GB")
                val matcher = pattern.matcher(html)
                if (matcher.find()) {
                    val found = matcher.group(1)
                    finalUsage = found?.toDoubleOrNull()
                }
            }

            if (finalUsage != null) {
                Result.success(finalUsage)
            } else {
                Log.e(TAG, "Parse error. Title: ${dashboardDoc.title()}")
                Result.failure(IOException("Usage element not found"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
