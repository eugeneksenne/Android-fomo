package com.example.feature.camera

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Bundle
import android.util.Log
import android.view.Surface
import java.io.IOException
import java.nio.ByteBuffer

enum class VideoCodecType(val mimeType: String) {
    H264(MediaFormat.MIMETYPE_VIDEO_AVC),
    HEVC(MediaFormat.MIMETYPE_VIDEO_HEVC)
}

/**
 * FOMO Live Engine: Hardware-Accelerated Video Encoder (MediaCodec)
 * 
 * Encodes camera surface frames into AVC / H.264 or HEVC / H.265 video NAL units for 
 * low-latency transmission to the FOMO Broadcast Engine & Edge Distribution CDN.
 */
class VideoEncoder(
    private val width: Int = 1080,
    private val height: Int = 1920,
    private val bitRate: Int = 4_500_000, // 4.5 Mbps for crisp 1080p live stream
    private val frameRate: Int = 60,       // Target 60 FPS
    private val iFrameInterval: Int = 1,   // Keyframe every 1 second
    private val codecType: VideoCodecType = VideoCodecType.H264
) {

    private var encoder: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var isEncoderRunning = false
    private val bufferInfo = MediaCodec.BufferInfo()
    private var activeMimeType: String = codecType.mimeType

    // Listener for encoded video data packets
    var onEncodedPacketListener: ((ByteBuffer, MediaCodec.BufferInfo, Boolean) -> Unit)? = null

    /**
     * Initializes the hardware AVC/H.264 or HEVC/H.265 MediaCodec encoder and creates the input Surface.
     */
    fun prepare(): Surface {
        val format = MediaFormat.createVideoFormat(activeMimeType, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
            setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
        }

        try {
            encoder = MediaCodec.createEncoderByType(activeMimeType).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                inputSurface = createInputSurface()
            }
            Log.d(TAG, "MediaCodec $activeMimeType hardware encoder prepared ($width x $height @ $frameRate FPS, ${bitRate / 1000} kbps)")
        } catch (e: IOException) {
            if (activeMimeType == MediaFormat.MIMETYPE_VIDEO_HEVC) {
                Log.w(TAG, "HEVC hardware encoder unavailable, falling back to AVC (H.264)", e)
                activeMimeType = MediaFormat.MIMETYPE_VIDEO_AVC
                return prepare()
            }
            Log.e(TAG, "Failed to create MediaCodec encoder for $activeMimeType", e)
            throw e
        }

        return inputSurface ?: throw IllegalStateException("Failed to create input Surface for MediaCodec")
    }

    /**
     * Starts the hardware video encoding loop.
     */
    fun start() {
        val codec = encoder ?: run {
            Log.e(TAG, "Encoder not prepared prior to start()")
            return
        }

        codec.start()
        isEncoderRunning = true
        Log.d(TAG, "VideoEncoder hardware pipeline STARTED")
    }

    /**
     * Polls output buffers from the hardware encoder and dispatches H.264 packets.
     * Call this periodically or on an encoding worker thread.
     */
    fun drainEncoder(endOfStream: Boolean) {
        val codec = encoder ?: return

        if (endOfStream) {
            try {
                codec.signalEndOfInputStream()
            } catch (e: Exception) {
                Log.e(TAG, "Error signaling end of input stream", e)
            }
        }

        while (isEncoderRunning) {
            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break // No data available right now
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = codec.outputFormat
                Log.d(TAG, "Encoder output format changed: $newFormat")
            } else if (outputBufferIndex >= 0) {
                val encodedData = codec.getOutputBuffer(outputBufferIndex)
                if (encodedData != null) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        // Codec config data (SPS / PPS header parameter sets)
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size != 0) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)

                        val isKeyFrame = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
                        onEncodedPacketListener?.invoke(encodedData, bufferInfo, isKeyFrame)
                    }

                    codec.releaseOutputBuffer(outputBufferIndex, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d(TAG, "End of stream reached in VideoEncoder")
                        break
                    }
                }
            }
        }
    }

    /**
     * Requests an immediate IDR Keyframe from the hardware encoder (e.g. for new viewers or AI Director switches).
     */
    fun requestSyncFrame() {
        encoder?.let { codec ->
            val params = Bundle().apply {
                putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
            }
            codec.setParameters(params)
            Log.d(TAG, "Immediate IDR Keyframe requested from MediaCodec")
        }
    }

    /**
     * Dynamically adjusts video bitrate according to real-time network conditions.
     */
    fun adjustBitrate(newBitrate: Int) {
        encoder?.let { codec ->
            val params = Bundle().apply {
                putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, newBitrate)
            }
            codec.setParameters(params)
            Log.d(TAG, "Adaptive Streaming: Bitrate dynamically adjusted to ${newBitrate / 1000} kbps")
        }
    }

    /**
     * Stops and releases hardware codec resources.
     */
    fun stopAndRelease() {
        isEncoderRunning = false
        try {
            encoder?.apply {
                stop()
                release()
            }
            inputSurface?.release()
            encoder = null
            inputSurface = null
            Log.d(TAG, "VideoEncoder hardware pipeline STOPPED & RELEASED")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaCodec encoder", e)
        }
    }

    companion object {
        private const val TAG = "FomoVideoEncoder"
        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC // H.264 Advanced Video Coding
        private const val TIMEOUT_USEC = 10_000L
    }
}
