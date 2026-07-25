package com.example.feature.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Sound Aware Engine — real on-device audio analysis.
 *
 * Replaces the previous placeholder, which set
 * `bpmValue = (120..128).random()` and never opened the microphone. The
 * "SOUND AWARE: n BPM" readout was therefore a random number, and the
 * beat-reactive effects had nothing to react to.
 *
 * Implementation notes:
 *  - Uses energy-based onset detection on the low band, which is what actually
 *    matters for the nightlife music this targets (kick drum / bass).
 *    A full FFT is unnecessary and more expensive; we band-limit by decimating
 *    and tracking short-term energy against a rolling average.
 *  - Tempo is derived from the median interval between detected onsets, which
 *    is far more robust to spurious beats than a mean.
 *  - Everything runs on-device (spec requirement); nothing is uploaded.
 *  - The engine degrades to silence if RECORD_AUDIO is not granted.
 *
 * Not implemented (documented, not faked): applause detection, vocal-peak
 * detection and drop prediction need a trained classifier rather than
 * energy heuristics.
 */
class SoundAwareEngine(private val context: Context) {

    data class SoundState(
        val isListening: Boolean = false,
        /** Detected tempo. Null until enough onsets have been observed. */
        val bpm: Int? = null,
        /** Normalised 0f..1f overall loudness. */
        val level: Float = 0f,
        /** Normalised 0f..1f low-band (bass) energy. */
        val bassLevel: Float = 0f,
        /** Rough crowd-energy read derived from sustained loudness. */
        val energy: Energy = Energy.QUIET,
    )

    enum class Energy { QUIET, BUILDING, ACTIVE, PEAK }

    private val _state = MutableStateFlow(SoundState())
    val state: StateFlow<SoundState> = _state.asStateFlow()

    /** Emits on every detected beat so the UI can pulse in time with the music. */
    private val _beats = MutableSharedFlow<Long>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val beats: SharedFlow<Long> = _beats.asSharedFlow()

    private var job: Job? = null

    /**
     * Held so [stop] can release the microphone synchronously. Cancelling the
     * coroutine alone is asynchronous: the analysis loop may still own the mic
     * when CameraX tries to claim it for recording, yielding a silent video.
     */
    @Volatile
    private var recorder: AudioRecord? = null

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // guarded by hasPermission()
    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        if (!hasPermission()) {
            Log.i(TAG, "RECORD_AUDIO not granted; Sound Aware stays idle.")
            return
        }

        job = scope.launch(Dispatchers.Default) {
            val minBuffer = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuffer <= 0) {
                Log.w(TAG, "AudioRecord unavailable on this device.")
                return@launch
            }

            val bufferSize = maxOf(minBuffer, FRAME_SAMPLES * 2)
            val record = try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unable to open the microphone", e)
                return@launch
            }

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.w(TAG, "AudioRecord failed to initialise.")
                runCatching { record.release() }
                return@launch
            }
            recorder = record

            val buffer = ShortArray(FRAME_SAMPLES)
            val energyHistory = ArrayDeque<Float>(ENERGY_HISTORY)
            val onsetTimes = ArrayDeque<Long>()
            var lastBeatAt = 0L

            try {
                record.startRecording()
                _state.value = _state.value.copy(isListening = true)

                while (isActive) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read <= 0) continue

                    val rms = rms(buffer, read)
                    val bass = lowBandEnergy(buffer, read)
                    val now = System.currentTimeMillis()

                    // Rolling average for adaptive thresholding: a club is loud
                    // everywhere, so an absolute threshold would fire constantly.
                    val average = if (energyHistory.isEmpty()) bass else energyHistory.average().toFloat()
                    energyHistory.addLast(bass)
                    if (energyHistory.size > ENERGY_HISTORY) energyHistory.removeFirst()

                    val isOnset = bass > average * ONSET_SENSITIVITY &&
                        bass > MIN_ONSET_ENERGY &&
                        (now - lastBeatAt) > MIN_BEAT_INTERVAL_MS

                    if (isOnset) {
                        lastBeatAt = now
                        onsetTimes.addLast(now)
                        while (onsetTimes.size > ONSET_WINDOW) onsetTimes.removeFirst()
                        _beats.tryEmit(now)
                    }

                    _state.value = _state.value.copy(
                        isListening = true,
                        bpm = estimateBpm(onsetTimes),
                        level = rms.coerceIn(0f, 1f),
                        bassLevel = bass.coerceIn(0f, 1f),
                        energy = classifyEnergy(rms)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Audio analysis stopped unexpectedly", e)
            } finally {
                releaseRecorder()
                _state.value = SoundState()
            }
        }
    }

    /**
     * Stops analysis and releases the microphone immediately, so another
     * client (CameraX) can take it without racing coroutine cancellation.
     */
    fun stop() {
        job?.cancel()
        job = null
        releaseRecorder()
        _state.value = SoundState()
    }

    @Synchronized
    private fun releaseRecorder() {
        val current = recorder ?: return
        recorder = null
        runCatching { if (current.recordingState == AudioRecord.RECORDSTATE_RECORDING) current.stop() }
        runCatching { current.release() }
    }

    companion object {
        private const val TAG = "SoundAware"

        private const val SAMPLE_RATE = 44_100
        private const val FRAME_SAMPLES = 2048
        private const val ENERGY_HISTORY = 43           // ~2 s of frames
        private const val ONSET_WINDOW = 16
        private const val ONSET_SENSITIVITY = 1.35f
        private const val MIN_ONSET_ENERGY = 0.02f
        private const val MIN_BEAT_INTERVAL_MS = 240L   // caps at ~250 BPM

        /** Root-mean-square amplitude, normalised to 0f..1f. */
        internal fun rms(buffer: ShortArray, length: Int): Float {
            if (length <= 0) return 0f
            var sum = 0.0
            for (i in 0 until length) {
                val v = buffer[i] / 32768.0
                sum += v * v
            }
            return sqrt(sum / length).toFloat()
        }

        /**
         * Low-band energy via a simple moving-average low-pass, which
         * suppresses highs and leaves the kick/bass content that drives
         * beat detection in dance music.
         */
        internal fun lowBandEnergy(buffer: ShortArray, length: Int): Float {
            if (length <= 0) return 0f
            var acc = 0.0
            var smoothed = 0.0
            val alpha = 0.15 // ~ low-pass cutoff
            for (i in 0 until length) {
                val v = buffer[i] / 32768.0
                smoothed += alpha * (v - smoothed)
                acc += abs(smoothed)
            }
            return (acc / length).toFloat()
        }

        /**
         * Median inter-onset interval → BPM.
         *
         * The median rejects the occasional missed or doubled beat that would
         * badly skew a mean.
         */
        internal fun estimateBpm(onsets: Collection<Long>): Int? {
            if (onsets.size < 4) return null
            val times = onsets.toList()
            val intervals = times.zipWithNext { a, b -> b - a }.filter { it > 0 }
            if (intervals.size < 3) return null

            val sorted = intervals.sorted()
            val median = if (sorted.size % 2 == 0) {
                (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
            } else {
                sorted[sorted.size / 2].toDouble()
            }
            if (median <= 0) return null

            var bpm = 60_000.0 / median
            // Fold into the musically sensible 70-180 range: energy onset
            // detection commonly locks onto half- or double-time.
            while (bpm < 70) bpm *= 2
            while (bpm > 180) bpm /= 2
            return bpm.roundToInt()
        }

        internal fun classifyEnergy(rms: Float): Energy = when {
            rms < 0.02f -> Energy.QUIET
            rms < 0.08f -> Energy.BUILDING
            rms < 0.20f -> Energy.ACTIVE
            else -> Energy.PEAK
        }
    }
}
