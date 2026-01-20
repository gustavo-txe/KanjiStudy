package com.app.kanjistudy.di

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        //Inicializar Ads posteriormente
        /*MobileAds.initialize(this)

        InterstitialAdManager.loadAd(
            this,
            "ca-app-pub-3940256099942544/1033173712" // test ID
        )*/

    }
}
