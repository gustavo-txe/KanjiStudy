package com.app.kanjistudy.core.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import com.app.kanjistudy.R

class CustomTab {
    fun openCustomTab(context: Context, url: String) {
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        try {
            customTabsIntent.launchUrl(context, Uri.parse(url))
        } catch (exception: ActivityNotFoundException) {
            Toast.makeText(context, R.string.browser_unavailable, Toast.LENGTH_LONG).show()
        }
    }
}
