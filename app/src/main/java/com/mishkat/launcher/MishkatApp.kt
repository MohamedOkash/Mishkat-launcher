package com.mishkat.launcher

import android.app.Application
import com.mishkat.launcher.core.AppGraph

class MishkatApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this).also { it.start() }
    }
}
