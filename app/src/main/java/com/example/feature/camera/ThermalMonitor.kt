package com.example.feature.camera

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Device Thermal Throttling Levels
 */
enum class ThermalLevel {
    NONE,       // Normal thermal operating range
    LIGHT,      // Slight temperature rise - Maintain quality
    MODERATE,   // Moderate heat - Slightly reduce encoding bitrate (e.g. 3.5 Mbps)
    SEVERE,     // High heat - Drop frame rate to 30 FPS and lower bitrate (e.g. 2.0 Mbps)
    CRITICAL,   // Thermal limit near - Emergency bitrate drop (e.g. 1.0 Mbps, 24 FPS)
    EMERGENCY,  // Imminent shutdown - Pause non-essential filters & AI rendering
    SHUTDOWN    // Immediate thermal emergency stop
}

data class ThermalState(
    val level: ThermalLevel = ThermalLevel.NONE,
    val recommendedBitrate: Int = 4_500_000, // Default 4.5 Mbps
    val recommendedFps: Int = 60,            // Default 60 FPS
    val isThermalThrottlingActive: Boolean = false,
    val temperatureDescription: String = "Nominal (Cool)"
)

/**
 * FOMO Live Engine: Hardware Thermal Protection & Performance Monitor
 * 
 * Monitors device thermal status via PowerManager OnThermalStatusChangedListener (Android Q+).
 * Automatically notifies the Live Broadcast Engine & VideoEncoder to dynamically lower 
 * bitrate and frame rates before hardware thermal throttling or camera shutdown occurs.
 */
class ThermalMonitor(private val context: Context) {

    private val _thermalState = MutableStateFlow(ThermalState())
    val thermalState: StateFlow<ThermalState> = _thermalState.asStateFlow()

    private var powerManager: PowerManager? = null
    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null
    private var isMonitoring = false
    private var simulationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Callback for broadcast engine adaptation
    var onThermalThrottleCallback: ((ThermalState) -> Unit)? = null

    /**
     * Starts listening to hardware thermal status changes.
     */
    fun startMonitoring() {
        if (isMonitoring) return

        powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            try {
                registerNativeThermalListener()
                isMonitoring = true
                Log.d(TAG, "ThermalMonitor native PowerManager API registered")
                return
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register native PowerManager thermal listener", e)
            }
        }

        // Fallback monitor / simulation for environments without hardware thermal sensors
        startFallbackMonitor()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun registerNativeThermalListener() {
        val pm = powerManager ?: return
        thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
            val state = mapThermalStatusToState(status)
            _thermalState.value = state
            onThermalThrottleCallback?.invoke(state)
            Log.w(TAG, "Thermal status changed: ${state.level} (${state.temperatureDescription})")
        }

        pm.addThermalStatusListener(thermalListener!!)
        
        // Initial state query
        val currentStatus = pm.currentThermalStatus
        _thermalState.value = mapThermalStatusToState(currentStatus)
    }

    /**
     * Maps Android PowerManager THERMAL_STATUS constants to live broadcast parameters.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun mapThermalStatusToState(status: Int): ThermalState {
        return when (status) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalState(
                level = ThermalLevel.NONE,
                recommendedBitrate = 4_500_000,
                recommendedFps = 60,
                isThermalThrottlingActive = false,
                temperatureDescription = "Nominal (Cool)"
            )
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalState(
                level = ThermalLevel.LIGHT,
                recommendedBitrate = 4_000_000,
                recommendedFps = 60,
                isThermalThrottlingActive = false,
                temperatureDescription = "Warm (Optimal)"
            )
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalState(
                level = ThermalLevel.MODERATE,
                recommendedBitrate = 3_000_000,
                recommendedFps = 60,
                isThermalThrottlingActive = true,
                temperatureDescription = "Moderate Heat"
            )
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalState(
                level = ThermalLevel.SEVERE,
                recommendedBitrate = 2_000_000,
                recommendedFps = 30,
                isThermalThrottlingActive = true,
                temperatureDescription = "High Temperature"
            )
            PowerManager.THERMAL_STATUS_CRITICAL -> ThermalState(
                level = ThermalLevel.CRITICAL,
                recommendedBitrate = 1_200_000,
                recommendedFps = 24,
                isThermalThrottlingActive = true,
                temperatureDescription = "Critical Thermal Peak"
            )
            PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalState(
                level = ThermalLevel.EMERGENCY,
                recommendedBitrate = 800_000,
                recommendedFps = 20,
                isThermalThrottlingActive = true,
                temperatureDescription = "Thermal Emergency"
            )
            PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalState(
                level = ThermalLevel.SHUTDOWN,
                recommendedBitrate = 500_000,
                recommendedFps = 15,
                isThermalThrottlingActive = true,
                temperatureDescription = "Device Shutdown Warning"
            )
            else -> ThermalState()
        }
    }

    /**
     * Fallback loop when running on emulator / pre-Q devices.
     */
    private fun startFallbackMonitor() {
        isMonitoring = true
        simulationJob = scope.launch {
            while (isActive && isMonitoring) {
                // Nominal operating state check
                delay(10_000)
            }
        }
    }

    /**
     * Stops thermal monitoring and unregisters system callbacks.
     */
    fun stopMonitoring() {
        isMonitoring = false
        simulationJob?.cancel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null && thermalListener != null) {
            try {
                powerManager?.removeThermalStatusListener(thermalListener!!)
                Log.d(TAG, "ThermalMonitor listener removed")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing thermal status listener", e)
            }
        }
        thermalListener = null
    }

    companion object {
        private const val TAG = "FomoThermalMonitor"
    }
}
