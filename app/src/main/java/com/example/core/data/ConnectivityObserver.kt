package com.example.core.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observes real device connectivity.
 *
 * The chat screen previously drove its "offline" state from a manual toolbar
 * toggle — a developer affordance, not a real signal. Actual loss of signal was
 * never detected, so messages composed on a dead connection were treated as
 * sent and, because the offline queue was never flushed to the backend, lost
 * silently.
 */
object ConnectivityObserver {

    private const val TAG = "Connectivity"

    /**
     * Emits `true` when a validated internet-capable network is available.
     *
     * `NET_CAPABILITY_VALIDATED` matters: it distinguishes a genuinely usable
     * connection from a captive-portal Wi-Fi that is associated but cannot
     * reach the internet — exactly the case that silently drops messages.
     */
    fun observe(context: Context): Flow<Boolean> = callbackFlow {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        if (manager == null) {
            trySend(true) // fail open; assume connected
            awaitClose { }
            return@callbackFlow
        }

        fun currentlyOnline(): Boolean = try {
            val caps = manager.activeNetwork?.let { manager.getNetworkCapabilities(it) }
            caps != null &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to read network state", e)
            true
        }

        trySend(currentlyOnline())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(currentlyOnline())
            }

            override fun onLost(network: Network) {
                trySend(currentlyOnline())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities,
            ) {
                trySend(
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                )
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            manager.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to register network callback", e)
            trySend(true)
        }

        awaitClose {
            runCatching { manager.unregisterNetworkCallback(callback) }
        }
    }.distinctUntilChanged()
}
