package com.example.rakutenmonitor.data

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.ArrayList

class SimpleCookieJar : CookieJar {
    private val cookies = ArrayList<Cookie>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val iterator = this.cookies.iterator()
        while (iterator.hasNext()) {
            val currentCookie = iterator.next()
            for (newCookie in cookies) {
                if (currentCookie.name == newCookie.name && 
                    currentCookie.domain == newCookie.domain && 
                    currentCookie.path == newCookie.path) {
                    iterator.remove()
                    break
                }
            }
        }
        this.cookies.addAll(cookies)
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
