package com.mishkat.remote

import android.app.Application
import com.mishkat.remote.discovery.TvDiscoveryClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class RemoteApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var discoveryClient: TvDiscoveryClient
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        discoveryClient = TvDiscoveryClient(this, appScope)
    }

    companion object {
        lateinit var instance: RemoteApp
            private set
    }
}
