package com.bopape.studentassistant.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks whether the device currently has an internet connection and exposes it
 * as a StateFlow. The UI shows an "offline" banner from it, and the app starts a
 * sync the moment the connection comes back.
 *
 * Reference: Android Developers (2026) Monitor connectivity status.
 * https://developer.android.com/training/basics/network-ops/reading-network-state
 */
class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkNow())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val listeners = mutableListOf<() -> Unit>()

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = setOnline(true)
            override fun onLost(network: Network) = setOnline(checkNow())
            override fun onUnavailable() = setOnline(false)
        })
    }

    /** Runs [listener] every time the device goes from offline to online. */
    fun onReconnected(listener: () -> Unit) {
        listeners += listener
    }

    private fun setOnline(online: Boolean) {
        val wasOnline = _isOnline.value
        _isOnline.value = online
        if (online != wasOnline) Log.i(TAG, if (online) "Connection restored" else "Connection lost")
        if (online && !wasOnline) listeners.forEach { it() }
    }

    private fun checkNow(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private companion object {
        const val TAG = "NetworkMonitor"
    }
}
