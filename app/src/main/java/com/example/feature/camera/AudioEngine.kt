package com.example.feature.camera

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Sound-Aware Telemetry Data Class for Sound-Reactive UI Effects
 */
data class AudioTelemetry(
    val bpm: Int = 128,
    val bassEnergy: Float = 0.75f,      // 0.0f - 1.0f (Sub-bass & Bass: 20 - 250 Hz)
    val midEnergy: Float = 0.55f,       // 0.0f - 1.0f (Mids: 250 - 2000 Hz)
    val trebleEnergy: Float = 0.40f,    // 0.0f - 1.0f (Highs: 2k - 10k Hz)
    val beatIntensity: Float = 0.82f,   // Instantaneous beat pulse intensity (0.0f - 1.0f)
    val isBeatDropDetected: Boolean = false,
    val crowdEnergyLevel: Int = 88,     // 0 - 100%
    val rhythmScore: Float = 0.92f,
    val activeVibe: String = "High Energy Amapiano / Afro-House"
)

/**
 * Oboe C++/JNI Native Engine Bridge.
 * Attempts to load 'oboe-engine' native library if present, or falls back seamlessly to
 * Android low-latency PCM spectral DSP pipeline.
 */
object OboeNativeEngine {
    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("oboe-engine")
            isNativeLoaded = true
            Log.i("OboeNativeEngine", "Oboe C++/JNI library loaded successfully.")
        } catch (e: Throwable) {
            isNativeLoaded = false
            Log.w("OboeNativeEngine", "Oboe C++ library not found; operating on high-performance Kotlin/PCM DSP pipeline.")
        }
    }

    fun isAvailable(): Boolean = isNativeLoaded

    external fun nativeInitStream(sampleRate: Int, framesPerBuffer: Int): Boolean
    external fun nativeStartStream(): Boolean
    external fun nativeStopStream()
    external fun nativeProcessAudioFrame(buffer: ShortArray, size: Int): FloatArray
}

/**
 * FOMO Sound-Aware Live Engine: Low-Latency Audio Processor, Spectral Analyzer & Beat Engine
 * 
 * Captures low-latency PCM audio stream, performs real-time frequency band DSP spectral analysis,
 * detects musical BPM, bass energy, beat drop events, and crowd volume dynamics.
 */
class AudioEngine {

    private val _telemetry = MutableStateFlow(AudioTelemetry())
    val telemetry: StateFlow<AudioTelemetry> = _telemetry.asStateFlow()

    // Real-time animation callback listeners for sound-reactive UI components
    private var onBeatIntensityListener: ((intensity: Float, bass: Float, mid: Float, treble: Float) -> Unit)? = null
    private var onBeatDropListener: (() -> Unit)? = null

    /**
     * Registers a listener callback to receive instantaneous beat intensity and band energies (60Hz loop).
     */
    fun setOnBeatIntensityListener(listener: ((intensity: Float, bass: Float, mid: Float, treble: Float) -> Unit)?) {
        this.onBeatIntensityListener = listener
    }

    /**
     * Registers a listener callback triggered on sub-bass beat drops.
     */
    fun setOnBeatDropListener(listener: (() -> Unit)?) {
        this.onBeatDropListener = listener
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var processingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // DSP Energy & Peak History for Beat Detection & Spectral Bands
    private val bassHistory = FloatArray(16)
    private var historyIndex = 0
    private var lastBeatTimestamp = System.currentTimeMillis()
    private val beatIntervals = ArrayList<Long>()

    /**
     * Starts low-latency audio capture and real-time DSP spectrum analysis.
     */
    @SuppressLint("MissingPermission")
    fun startListening() {
        if (isRecording) return

        val sampleRate = SAMPLE_RATE
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        if (OboeNativeEngine.isAvailable()) {
            OboeNativeEngine.nativeInitStream(sampleRate, BUFFER_SIZE)
            OboeNativeEngine.nativeStartStream()
            Log.i(TAG, "Initialized Oboe C++ Native Engine stream.")
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize.coerceAtLeast(BUFFER_SIZE)
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed. Falling back to simulated live ambient feed.")
                startFallbackAnalysis()
                return
            }

            audioRecord?.startRecording()
            isRecording = true
            Log.d(TAG, "AudioEngine low-latency stream STARTED ($sampleRate Hz)")

            processingJob = scope.launch {
                val buffer = ShortArray(BUFFER_SIZE)
                while (isActive && isRecording) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        analyzePcmFrame(buffer, readSize)
                    }
                    delay(30) // ~33 Hz DSP spectral analysis loop
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioRecord microphone capture", e)
            startFallbackAnalysis()
        }
    }

    /**
     * Performs DSP audio spectral analysis on PCM frame buffer.
     * Uses Oboe C++/JNI engine if loaded, otherwise executes low-latency Kotlin DSP spectrum pipeline.
     */
    private fun analyzePcmFrame(buffer: ShortArray, readSize: Int) {
        if (OboeNativeEngine.isAvailable()) {
            val nativeResult = OboeNativeEngine.nativeProcessAudioFrame(buffer, readSize)
            if (nativeResult != null && nativeResult.size >= 6) {
                val bassEnergy = nativeResult[0]
                val midEnergy = nativeResult[1]
                val trebleEnergy = nativeResult[2]
                val beatIntensity = nativeResult[3]
                val bpm = nativeResult[4].toInt()
                val isBeatDrop = nativeResult[5] > 0.5f

                onBeatIntensityListener?.invoke(beatIntensity, bassEnergy, midEnergy, trebleEnergy)
                if (isBeatDrop) {
                    onBeatDropListener?.invoke()
                }

                _telemetry.value = AudioTelemetry(
                    bpm = bpm,
                    bassEnergy = bassEnergy,
                    midEnergy = midEnergy,
                    trebleEnergy = trebleEnergy,
                    beatIntensity = beatIntensity,
                    isBeatDropDetected = isBeatDrop,
                    crowdEnergyLevel = ((bassEnergy * 80) + 20).toInt().coerceIn(20, 100),
                    rhythmScore = (bassEnergy * 0.95f).coerceIn(0.5f, 0.99f),
                    activeVibe = when {
                        bpm >= 135 -> "High Peak Technos / Hard Bass"
                        bpm in 120..134 -> "Amapiano / House Beat Peak"
                        else -> "Lounge Chill & Crowd Ambient"
                    }
                )
                return
            }
        }

        var sumSquares = 0.0
        var peakValue = 0

        // Band energy integration via simplified Goertzel filters for Low (100Hz), Mid (1000Hz), High (4000Hz)
        var bassSum = 0.0
        var midSum = 0.0
        var trebleSum = 0.0

        val kBass = (2.0 * cos(2.0 * Math.PI * 100.0 / SAMPLE_RATE))
        val kMid = (2.0 * cos(2.0 * Math.PI * 1000.0 / SAMPLE_RATE))
        val kTreble = (2.0 * cos(2.0 * Math.PI * 4000.0 / SAMPLE_RATE))

        var sPrevBass1 = 0.0
        var sPrevBass2 = 0.0
        var sPrevMid1 = 0.0
        var sPrevMid2 = 0.0
        var sPrevTreble1 = 0.0
        var sPrevTreble2 = 0.0

        for (i in 0 until readSize) {
            val sample = buffer[i].toDouble() / 32768.0
            sumSquares += (sample * sample)
            if (abs(buffer[i].toInt()) > peakValue) {
                peakValue = abs(buffer[i].toInt())
            }

            // Goertzel recurrences
            val sBass = sample + kBass * sPrevBass1 - sPrevBass2
            sPrevBass2 = sPrevBass1
            sPrevBass1 = sBass

            val sMid = sample + kMid * sPrevMid1 - sPrevMid2
            sPrevMid2 = sPrevMid1
            sPrevMid1 = sMid

            val sTreble = sample + kTreble * sPrevTreble1 - sPrevTreble2
            sPrevTreble2 = sPrevTreble1
            sPrevTreble1 = sTreble
        }

        val rms = sqrt(sumSquares / readSize)
        val normalizedEnergy = rms.toFloat().coerceIn(0f, 1f)

        // Goertzel power calculation
        val bassPower = sqrt(sPrevBass1 * sPrevBass1 + sPrevBass2 * sPrevBass2 - kBass * sPrevBass1 * sPrevBass2) / readSize
        val midPower = sqrt(sPrevMid1 * sPrevMid1 + sPrevMid2 * sPrevMid2 - kMid * sPrevMid1 * sPrevMid2) / readSize
        val treblePower = sqrt(sPrevTreble1 * sPrevTreble1 + sPrevTreble2 * sPrevTreble2 - kTreble * sPrevTreble1 * sPrevTreble2) / readSize

        val bassEnergy = ((bassPower * 12.0) + (normalizedEnergy * 1.2)).toFloat().coerceIn(0f, 1f)
        val midEnergy = ((midPower * 18.0) + (normalizedEnergy * 0.8)).toFloat().coerceIn(0f, 1f)
        val trebleEnergy = ((treblePower * 25.0) + (normalizedEnergy * 0.5)).toFloat().coerceIn(0f, 1f)

        // Store history for peak beat detection
        bassHistory[historyIndex] = bassEnergy
        historyIndex = (historyIndex + 1) % bassHistory.size

        val averageBass = bassHistory.average().toFloat()
        val now = System.currentTimeMillis()

        // Beat Drop & Peak Detection Logic
        var isBeatDrop = false
        var beatIntensity = (bassEnergy * 0.7f + normalizedEnergy * 0.3f).coerceIn(0f, 1f)

        if (bassEnergy > (averageBass * 1.5f) && bassEnergy > 0.40f) {
            beatIntensity = 1.0f
            if (now - lastBeatTimestamp > 320) { // Max ~187 BPM cap
                val interval = now - lastBeatTimestamp
                lastBeatTimestamp = now
                if (interval in 300..1000) {
                    beatIntervals.add(interval)
                    if (beatIntervals.size > 8) beatIntervals.removeAt(0)
                }

                // Check for dramatic beat drop (bass spike after quiet section)
                if (averageBass < 0.35f && bassEnergy > 0.7f) {
                    isBeatDrop = true
                    Log.d(TAG, "⚡ BEAT DROP DETECTED! Bass Spike: $bassEnergy")
                }
            }
        }

        // Calculate Average BPM from beat intervals
        val calculatedBpm = if (beatIntervals.isNotEmpty()) {
            val avgInterval = beatIntervals.average()
            if (avgInterval > 0) (60000 / avgInterval).toInt().coerceIn(70, 180) else 128
        } else {
            128
        }

        val crowdLevel = ((normalizedEnergy * 85) + 15).toInt().coerceIn(15, 100)

        onBeatIntensityListener?.invoke(beatIntensity, bassEnergy, midEnergy, trebleEnergy)
        if (isBeatDrop) {
            onBeatDropListener?.invoke()
        }

        _telemetry.value = AudioTelemetry(
            bpm = calculatedBpm,
            bassEnergy = bassEnergy,
            midEnergy = midEnergy,
            trebleEnergy = trebleEnergy,
            beatIntensity = beatIntensity,
            isBeatDropDetected = isBeatDrop,
            crowdEnergyLevel = crowdLevel,
            rhythmScore = (normalizedEnergy * 0.95f).coerceIn(0.5f, 0.99f),
            activeVibe = when {
                calculatedBpm >= 135 -> "High Peak Technos / Hard Bass"
                calculatedBpm in 120..134 -> "Amapiano / House Beat Peak"
                else -> "Lounge Chill & Crowd Ambient"
            }
        )
    }

    /**
     * Fallback simulated telemetry generator when hardware mic is unavailable.
     */
    private fun startFallbackAnalysis() {
        isRecording = true
        processingJob = scope.launch {
            var currentBpm = 128
            var angle = 0f
            while (isActive && isRecording) {
                angle += 0.2f
                val bassPulse = (abs(sin(angle.toDouble())) * 0.85f).toFloat()
                val midPulse = (abs(cos(angle * 1.5f)) * 0.6f).toFloat()
                val treblePulse = (abs(sin(angle * 3f)) * 0.4f).toFloat()
                val isDrop = bassPulse > 0.8f && (System.currentTimeMillis() % 8000 < 100)

                _telemetry.value = AudioTelemetry(
                    bpm = currentBpm,
                    bassEnergy = bassPulse,
                    midEnergy = midPulse,
                    trebleEnergy = treblePulse,
                    beatIntensity = bassPulse,
                    isBeatDropDetected = isDrop,
                    crowdEnergyLevel = 92,
                    rhythmScore = 0.94f,
                    activeVibe = "Truth Amapiano Live Set"
                )
                delay(100)
            }
        }
    }

    /**
     * Stops audio capture and frees hardware resources.
     */
    fun stopListening() {
        isRecording = false
        processingJob?.cancel()
        try {
            audioRecord?.apply {
                if (state == AudioRecord.STATE_INITIALIZED) {
                    stop()
                }
                release()
            }
            audioRecord = null
            Log.d(TAG, "AudioEngine audio capture STOPPED")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }
    }

    companion object {
        private const val TAG = "FomoAudioEngine"
        private const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE = 2048
    }
}

