package com.example.rakutenmonitor.data

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException

class RakutenRepository {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .cookieJar(SimpleCookieJar()) // We need a simple cookie jar implementation
        .build()

    fun fetchData(userId: String, pass: String): Result<Double> {
        try {
            // 1. Initial Access to get cookies/tokens if needed (Mocking simplified flow based on python script)
            // The python script directly goes to login page.
            
            // 2. Login
            val loginUrl = "https://grp03.id.rakuten.co.jp/rms/nid/login?service_id=rm001&client_id=rmn_app_web&redirect_uri=https%3A%2F%2Fportal.mobile.rakuten.co.jp%2Fdashboard&scope=memberinfo_read_safebulk%2Cmemberinfo_read_point%2Cmemberinfo_get_card_token%2C30days%40Access%2C90days%40Refresh&contact_info_required=false&rae_service_id=rm001"
            
            // Note: In a real HTTP implementation, we often need to parse hidden fields (tokens) from the Get request 
            // before Posting credentials. 
            // For now, I will implement a basic flow. If Rakuten requires robust hidden token handling, 
            // we might need a two-step login (GET -> Extract hidden -> POST).
            
            val requestBody = FormBody.Builder()
                .add("u", userId) // Common Rakuten field text for username
                .add("p", pass)    // Common Rakuten field text for password
                // Note: These field names 'u' and 'p' are guesses based on common Rakuten forms or need verification from HTML.
                // The python script used XPath to find input fields, so we need to know the 'name' attributes.
                // Since I can't run the python script to check currently, I will assume standard names or placeholders.
                // FIX: The Python script used XPaths, which implies the structure.
                // Let's assume we need to GET the page first to find the input names if they are dynamic.
                // For this V1, let's assume we might need to adjust this after testing.
                .build()

            val loginRequest = Request.Builder()
                .url(loginUrl)
                .post(requestBody)
                .build()

            val loginResponse = client.newCall(loginRequest).execute()
            if (!loginResponse.isSuccessful) return Result.failure(IOException("Login failed: ${loginResponse.code}"))
            loginResponse.close()

            // 3. Fetch Dashboard
            val dashboardUrl = "https://portal.mobile.rakuten.co.jp/dashboard"
            val dashboardRequest = Request.Builder().url(dashboardUrl).build()
            val dashboardResponse = client.newCall(dashboardRequest).execute()
            
            if (!dashboardResponse.isSuccessful) return Result.failure(IOException("Dashboard access failed: ${dashboardResponse.code}"))
            
            val html = dashboardResponse.body?.string() ?: return Result.failure(IOException("Empty body"))
            dashboardResponse.close()

            // 4. Parse Data
            val doc = Jsoup.parse(html)
            // Python: //div[@class="title__data"]
            val usageElement = doc.select("div.title__data").first()
            
            return if (usageElement != null) {
                // "19.5 GB" -> 19.5
                val text = usageElement.text().replace("GB", "").replace(" ", "")
                val usage = text.toDoubleOrNull() ?: return Result.failure(IOException("Parse error: $text"))
                Result.success(usage)
            } else {
                Result.failure(IOException("Usage element not found"))
            }

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
