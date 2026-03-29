package com.example.connect.mirror

import android.media.MediaCodec
import android.media.projection.MediaProjection
import kotlinx.coroutines.*
import android.util.Log

/**
 * MirrorSourceManager handles the sender-side mirroring logic.
 * It manages the MediaEncoder and UdpTransport for 60fps video transmission.
 */
class MirrorSourceManager(
    private val coroutineScope: CoroutineScope,
    private val width: Int,
    private val height: Int,
    private val densityDpi: Int
) {

    private var mediaEncoder: MediaEncoder? = null
    private var udpTransport: UdpTransport? = null
    private var streamingJob: Job? = null

    fun start(mediaProjection: MediaProjection, destinationAddress: String) {
        stop() // Ensure previous resources are cleaned
        
        mediaEncoder = MediaEncoder(width, height, densityDpi)
        udpTransport = UdpTransport(address = destinationAddress)
        try {
            udpTransport?.open()
        } catch (e: Exception) {
            Log.e("MirrorSourceManager", "Failed to open UDP Transport: ${e.message}")
            return
        }

        mediaEncoder?.prepare(mediaProjection) { info: MediaCodec.BufferInfo, data: ByteArray -> 
            // Callback implementation (if needed, currently encapsulated)
        }

        streamingJob = coroutineScope.launch(Dispatchers.IO) {
            val bufferInfo = MediaCodec.BufferInfo()
            try {
                while (isActive) {
                    val data = mediaEncoder?.dequeueOutput(bufferInfo)
                    if (data != null) {
                        udpTransport?.send(data)
                    }
                }
            } catch (e: Exception) {
                Log.e("MirrorSourceManager", "Streaming loop error: ${e.message}")
            } finally {
                internalCleanup()
            }
        }
    }

    fun updateBitrate(bitrate: Int) {
        mediaEncoder?.updateBitrate(bitrate)
    }

    /**
     * Stops the source process and releases resources.
     */
    fun stop() {
        streamingJob?.cancel()
        streamingJob = null
        internalCleanup()
    }

    private fun internalCleanup() {
        mediaEncoder?.release()
        udpTransport?.close()
        mediaEncoder = null
        udpTransport = null
    }
}
