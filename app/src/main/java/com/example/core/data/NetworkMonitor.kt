package com.example.core.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide connectivity monitor used by Discover sections to switch
 * between live, empty and offline states.
 *
 * The monitor defaults to online until it is initialised with a [Context].
 * UI previews, unit tests and misconfigured devices therefore never get
 * stuck in a false offline state.
 */
object NetworkMonitor {

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var initialized = false

    /**
     * Idempotent initialisation. Safe to call from any composable or
     * repository; only the first call registers the system callback.
     */
    fun init(context: Context) {
        if (initialized) return
        initialized = true

        val connectivityManager = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            _isOnline.value = true
            return
        }

        _isOnline.value = currentOnlineState(connectivityManager)

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _isOnline.value = true
                    }

                    override fun onLost(network: Network) {
                        _isOnline.value = currentOnlineState(connectivityManager)
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities
                    ) {
                        _isOnline.value =
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                                currentOnlineState(connectivityManager)
                    }
                }
            )
        } catch (securityException: SecurityException) {
            // Missing ACCESS_NETWORK_STATE at runtime: stay optimistic.
            _isOnline.value = true
        }
    }

    /** Re-evaluates the current connectivity snapshot (used by Retry actions). */
    fun refresh(context: Context) {
        val connectivityManager = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        _isOnline.value = currentOnlineState(connectivityManager)
    }

    private fun currentOnlineState(connectivityManager: ConnectivityManager): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
