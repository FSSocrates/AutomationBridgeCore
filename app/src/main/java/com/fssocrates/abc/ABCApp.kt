package com.fssocrates.abc

import android.app.Application
import timber.log.Timber

class ABCApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
