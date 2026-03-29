package com.example.connect.mirror

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import android.util.Log

/**
 * Handles hardware-accelerated AVC decoding for the Sink (Receiver) device.
 * Optimized for 60fps 1080p with minimal buffering.
 */
class MediaDecoder(private val surface: Surface) {

    private var decoder: MediaCodec? = null
    private var isConfigured = false

    fun configure(width: Int, height: Int) {
        try {
            val format = MediaFormat.createVideoFormat(MediaConstants.MIMETYPE_VIDEO_AVC, width, height).apply {
                // Low-latency decoding hint (API 30+)
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
                }
                // Help decoder estimate buffer requirements
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, width * height)
            }

            if (!surface.isValid) {
                Log.e("MediaDecoder", "Surface is invalid, cannot configure decoder")
                Log.e("Connect:CAUGHT", "Cannot configure MediaDecoder: Surface is INVALID")
                return
            }
            
            decoder = MediaCodec.createDecoderByType(MediaConstants.MIMETYPE_VIDEO_AVC)
            try {
                decoder?.configure(format, surface, null, 0)
                decoder?.start()
                Log.d("Connect:DEBUG", "MediaDecoder started successfully: $format")
            } catch (e: Exception) {
                Log.e("Connect:CAUGHT", "MediaCodec configuration FAILED: ${e.message}", e)
                throw e // Re-throw to be caught by the outer try-catch
            }
            isConfigured = true
            Log.d("MediaDecoder", "Decoder successfully configured for ${width}x${height}")
        } catch (e: Exception) {
            Log.e("MediaDecoder", "Failed to configure decoder: ${e.message}", e)
        }
    }

    /**
     * Decodes a chunk of encoded video data.
     * Handles late configuration if SPS/PPS are detected in the bitstream.
     */
    fun decodeFrame(data: ByteArray, presentationTimeUs: Long) {
        if (!isConfigured) {
            checkForConfig(data)
            if (!isConfigured) return // Skip until we have config
        }

        val codec = decoder ?: return
        try {
            val inputIndex = codec.dequeueInputBuffer(10000)
            if (inputIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputIndex)
                inputBuffer?.clear()
                inputBuffer?.put(data)
                codec.queueInputBuffer(inputIndex, 0, data.size, presentationTimeUs, 0)
            }

            val bufferInfo = MediaCodec.BufferInfo()
            var outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
            while (outputIndex >= 0) {
                codec.releaseOutputBuffer(outputIndex, true)
                outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
            }
        } catch (e: Exception) {
            Log.e("MediaDecoder", "Decode error: ${e.message}")
            if (e is MediaCodec.CodecException && e.isRecoverable) {
                // Potential need to re-configure
                isConfigured = false
            }
        }
    }

    private fun checkForConfig(data: ByteArray) {
        // Simple NAL scanner for SPS (0x67) and PPS (0x68)
        // Usually found at the start of the first I-frame
        var sps: ByteArray? = null
        var pps: ByteArray? = null

        var i = 0
        while (i < data.size - 5) {
            if (data[i] == 0.toByte() && data[i+1] == 0.toByte() && data[i+2] == 0.toByte() && data[i+3] == 1.toByte()) {
                val type = data[i+4].toInt() and 0x1F
                if (type == 7) { // SPS
                    sps = findNalLimit(data, i + 4)
                } else if (type == 8) { // PPS
                    pps = findNalLimit(data, i + 4)
                }
            }
            i++
        }

        if (sps != null && pps != null) {
            Log.i("MediaDecoder", "🎯 SPS/PPS Detected in bitstream. Performing Late Configuration.")
            configureDynamic(sps, pps)
        }
    }

    private fun findNalLimit(data: ByteArray, start: Int): ByteArray {
        var end = start
        while (end < data.size - 4) {
            if (data[end] == 0.toByte() && data[end+1] == 0.toByte() && data[end+2] == 0.toByte() && data[end+3] == 1.toByte()) {
                break
            }
            end++
        }
        if (end >= data.size - 4) end = data.size
        return data.copyOfRange(start - 4, end)
    }

    private fun configureDynamic(sps: ByteArray, pps: ByteArray) {
        try {
            val format = MediaFormat.createVideoFormat(MediaConstants.MIMETYPE_VIDEO_AVC, 1920, 1080).apply {
                setByteBuffer("csd-0", java.nio.ByteBuffer.wrap(sps))
                setByteBuffer("csd-1", java.nio.ByteBuffer.wrap(pps))
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
                }
            }
            
            decoder?.stop()
            decoder?.release()
            
            decoder = MediaCodec.createDecoderByType(MediaConstants.MIMETYPE_VIDEO_AVC)
            decoder?.configure(format, surface, null, 0)
            decoder?.start()
            isConfigured = true
            Log.d("MediaDecoder", "Late Dynamic Configuration SUCCESSFUL")
        } catch (e: Exception) {
            Log.e("MediaDecoder", "Late Configuration FAILED: ${e.message}")
        }
    }

    fun release() {
        try {
            decoder?.stop()
            decoder?.release()
            decoder = null
            isConfigured = false
        } catch (e: Exception) {
            Log.e("MediaDecoder", "Release error: ${e.message}")
        }
    }
}
