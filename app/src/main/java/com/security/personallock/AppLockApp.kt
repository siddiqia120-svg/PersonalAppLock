package com.security.personallock

import android.app.Application

class AppLockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: AppLockApp
            private set
    }
}
