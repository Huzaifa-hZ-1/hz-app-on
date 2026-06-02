package com.hz.appon.shared

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Observes network connectivity and exposes it as a [StateFlow].
 *
 * Call [register] in Application.onCreate and [unregister] if needed.
 * Activities and ViewModels collect [isOnline] to react to connectivity changes.
 */
class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    /** True when the device has an active internet-capable network connection. */
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Timber.d("Network available: $network")
            _isOnline.value = true
        }

        override fun onLost(network: Network) {
            Timber.d("Network lost: $network")
            _isOnline.value = checkCurrentConnectivity()
        }
    }

    /** Registers the network callback. Call once from [App.onCreate]. */
    fun register() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        Timber.d("NetworkMonitor registered, online=${_isOnline.value}")
    }

    /** Unregisters the network callback. Call when monitoring is no longer needed. */
    fun unregister() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private fun checkCurrentConnectivity(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
