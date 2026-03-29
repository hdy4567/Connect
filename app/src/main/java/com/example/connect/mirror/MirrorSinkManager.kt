package com.example.connect.mirror

import android.view.Surface
import kotlinx.coroutines.*
import android.util.Log
import com.example.connect.config.NetworkConfig

/**
 * MirrorSinkManager handles the receiver-side mirroring logic.
 * It manages the MediaDecoder and UdpTransport for 60fps video reception.
 */
class MirrorSinkManager(
    private val coroutineScope: CoroutineScope,
    private val onMetricsUpdate: (Int, Long) -> Unit = { _, _ -> }
) {

    private var mediaDecoder: MediaDecoder? = null
    private var udpTransport: UdpTransport? = null
    private var sinkJob: Job? = null

    fun start(surface: Surface, width: Int = MediaConstants.DEFAULT_WIDTH, height: Int = MediaConstants.DEFAULT_HEIGHT, onFirstFrame: () -> Unit = {}) {
        stop() // Ensure previous resources are cleaned
        
        mediaDecoder = MediaDecoder(surface).apply {
            configure(width, height)
        }
        
        // UDP Transport setup
        try {
            udpTransport = UdpTransport().apply {
                open(NetworkConfig.DEFAULT_DEST_PORT)
            }
        } catch (e: Exception) {
            Log.e("Connect:CAUGHT", "Failed to open UDP port ${NetworkConfig.DEFAULT_DEST_PORT}: ${e.message}")
            return
        }

        sinkJob = coroutineScope.launch(Dispatchers.IO) {
            var firstFrame = true
            var frameCount = 0
            var lastTime = System.currentTimeMillis()
            Log.d("Connect:DEBUG", "Starting UDP sink receiver on port ${NetworkConfig.DEFAULT_DEST_PORT}")
            val buffer = ByteArray(65535) 
            
            try {
                while (isActive) {
                    val startTime = System.nanoTime()
                    val len = udpTransport?.receive(buffer) ?: -1
                    if (len > 0) {
                        val frameData = buffer.copyOf(len)
                        mediaDecoder?.decodeFrame(frameData, System.nanoTime() / 1000)
                        
                        frameCount++
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastTime >= 1000) {
                            val latency = (System.nanoTime() - startTime) / 1000000
                            onMetricsUpdate(frameCount, latency)
                            frameCount = 0
                            lastTime = currentTime
                        }
                        
                        if (firstFrame) {
                            withContext(Dispatchers.Main) { onFirstFrame() }
                            firstFrame = false
                        }
                    } else if (len == -1) {
                        Log.w("Connect:DEBUG", "UDP receive loop terminated or timed out")
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("MirrorSinkManager", "Sink loop error: ${e.message}")
            } finally {
                internalCleanup()
            }
        }
    }

    /**
     * Stops the sink process and releases resources.
     */
    fun stop() {
        sinkJob?.cancel()
        sinkJob = null
        internalCleanup()
    }

    private fun internalCleanup() {
        mediaDecoder?.release()
        udpTransport?.close()
        mediaDecoder = null
        udpTransport = null
    }
}
