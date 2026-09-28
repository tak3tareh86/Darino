package com.example.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NetworkStateMonitor(
    private val context: Context,
    private val onNetworkRestored: (() -> Unit)? = null
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkInitialConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)
    private var debounceJob: Job? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            val wasOffline = !_isOnline.value
            _isOnline.value = true
            Log.i("NetworkStateMonitor", "Network available: connected to internet")

            if (wasOffline) {
                // Debounce trigger
                debounceJob?.cancel()
                debounceJob = scope.launch {
                    delay(1500) // 1.5s debounce
                    if (_isOnline.value) {
                        Log.i("NetworkStateMonitor", "Triggering sync after network restored")
                        onNetworkRestored?.invoke()
                    }
                }
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            _isOnline.value = checkInitialConnectivity()
            Log.w("NetworkStateMonitor", "Network connection lost")
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isOnline.value = hasInternet
        }
    }

    init {
        register()
    }

    private fun checkInitialConnectivity(): Boolean {
        if (connectivityManager == null) return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun register() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.e("NetworkStateMonitor", "Failed to register network callback", e)
        }
    }

    fun unregister() {
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
