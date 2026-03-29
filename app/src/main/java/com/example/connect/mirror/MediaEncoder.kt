package com.example.connect.mirror

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.view.Surface
import android.util.Log
import com.example.connect.config.MediaConfig

/**
 * Handles MediaCodec and VirtualDisplay setup for 60fps encoding.
 */
class MediaEncoder(private val width: Int, private val height: Int, private val density: Int) {
    
    private var encoder: MediaCodec? = null
    private var virtualDisplay: VirtualDisplay? = null

    fun prepare(mediaProjection: MediaProjection, onBufferReady: (MediaCodec.BufferInfo, ByteArray) -> Unit) : Surface {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, MediaConfig.BITRATE_8MBPS)
            setInteger(MediaFormat.KEY_FRAME_RATE, MediaConfig.FRAME_RATE_60)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, MediaConfig.I_FRAME_INTERVAL)
            setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
            setInteger(MediaFormat.KEY_LATENCY, 1)
            setInteger(MediaFormat.KEY_PRIORITY, 0)
        }

        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
        
        val inputSurface = encoder!!.createInputSurface()
        
        try {
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ConnectStream", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                inputSurface, null, null
            )
        } catch (e: Exception) {
            Log.e("Connect:CAUGHT", "Failed to create VirtualDisplay: ${e.message}. Mirroring source aborted.", e)
            throw RuntimeException("VirtualDisplay Creation Failure: ${e.message}", e)
        }
        
        encoder?.start()
        return inputSurface
    }

    fun updateBitrate(newBitrate: Int) {
        val params = android.os.Bundle().apply {
            putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, newBitrate)
        }
        encoder?.setParameters(params)
        Log.d("MediaEncoder", "Dynamic Bitrate Adjusted to: $newBitrate bps")
    }

    fun dequeueOutput(bufferInfo: MediaCodec.BufferInfo, timeoutUs: Long = 10000): ByteArray? {
        val index = encoder?.dequeueOutputBuffer(bufferInfo, timeoutUs) ?: -1
        if (index >= 0) {
            val buffer = encoder?.getOutputBuffer(index)
            val data = ByteArray(bufferInfo.size)
            buffer?.get(data)
            encoder?.releaseOutputBuffer(index, false)
            return data
        }
        return null
    }

    fun release() {
        virtualDisplay?.release()
        try {
            encoder?.stop()
            encoder?.release()
        } catch (e: Exception) {}
        encoder = null
    }
}
