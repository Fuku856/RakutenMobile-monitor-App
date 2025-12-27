package com.example.rakutenmonitor.data

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.ArrayList

class SimpleCookieJar : CookieJar {
    private val cookieStore = mutableListOf<Cookie>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val it = cookieStore.iterator()
        while (it.hasNext()) {
            val current = it.next()
            for (newCookie in cookies) {
                if (current.name == newCookie.name && current.domain == newCookie.domain && current.path == newCookie.path) {
                    it.remove()
                    break
                }
            }
        }
        cookieStore.addAll(cookies)
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val validCookies = mutableListOf<Cookie>()
        val it = cookieStore.iterator()
        while (it.hasNext()) {
            val cookie = it.next()
            if (cookie.expiresAt < System.currentTimeMillis()) {
                it.remove()
            } else if (cookie.matches(url)) {
                validCookies.add(cookie)
            }
        }
        return validCookies
    }
}
