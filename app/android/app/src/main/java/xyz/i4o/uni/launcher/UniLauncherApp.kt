package xyz.i4o.uni.launcher

import android.app.Application

class UniLauncherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Application-scoped native runtime
        LauncherRuntime.get(this)
    }
}

