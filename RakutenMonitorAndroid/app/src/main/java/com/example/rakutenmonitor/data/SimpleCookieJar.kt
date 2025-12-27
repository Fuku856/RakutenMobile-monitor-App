package com.example.rakutenmonitor.data

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.ArrayList

class SimpleCookieJar : CookieJar {
    private val cookies = ArrayList<Cookie>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        this.cookies.addAll(cookies)
        // Simple deduplication could be added here if needed, but for now we just accumulate
        // In a production app, you'd want to remove expired/duplicate cookies
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val matchingCookies = ArrayList<Cookie>()
        val iterator = cookies.iterator()
        while (iterator.hasNext()) {
            val cookie = iterator.next()
            if (cookie.expiresAt < System.currentTimeMillis()) {
                iterator.remove()
                continue
            }
            if (cookie.matches(url)) {
                matchingCookies.add(cookie)
            }
        }
        return matchingCookies
    }
}
