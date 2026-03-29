package com.example.connect.mirror

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaCodec
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import com.example.connect.config.MediaConfig
import com.example.connect.util.OnConnectionListener
import com.example.connect.util.WifiDirectManager
import android.net.wifi.p2p.WifiP2pDevice
import com.example.connect.logic.ThermalGuard
import com.example.connect.logic.ProtocolManager
import com.example.connect.mirror.HidDataSender
import com.example.connect.proto.AppState
import com.example.connect.proto.SessionState
import com.example.connect.util.ProtobufUtils
import com.example.connect.util.TouchInjector
import com.example.connect.logic.SyncStateBus
import com.example.connect.config.NetworkConfig
import java.net.InetAddress

/**
 * MirrorService acts as a high-level coordinator for screen mirroring.
 * It manages the lifecycle of MirrorSourceManager and WifiDirect connections.
 */
class MirrorService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var sourceManager: MirrorSourceManager? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var thermalGuard: ThermalGuard? = null
    
    private var protocolManager: ProtocolManager? = null
    private var hidDataSender: HidDataSender? = null
    private var touchInjector = TouchInjector()
    private var controlJob: Job? = null
    private var destinationIp: String = NetworkConfig.DEFAULT_DEST_ADDRESS
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        destinationIp = intent?.getStringExtra("DESTINATION_IP") ?: NetworkConfig.DEFAULT_DEST_ADDRESS
        
        // Protocol Manager Initialization
        if (protocolManager == null) {
            protocolManager = ProtocolManager(android.os.Build.MODEL)
            hidDataSender = HidDataSender(this)
        }

        when (action) {
            "START_SOURCE" -> {
                val resultCode = intent.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED)
                val resultData = intent.getParcelableExtra<Intent>("RESULT_DATA")
                if (resultData != null) {
                    startForegroundService("Source Mode Active")
                    serviceScope.launch {
                        delay(100)
                        withContext(Dispatchers.Main) {
                            setupMirroring(resultCode, resultData, destinationIp)
                        }
                    }
                    startControlChannel()
                }
            }
            "START_SINK" -> {
                startForegroundService("Sink Mode Active")
                startControlChannel()
            }
            "STOP_FUNCTION" -> {
                cleanupFunctions()
            }
            "STOP" -> {
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }

    private fun setupThermalGuard() {
        thermalGuard = ThermalGuard(this).apply {
            setListener(object : ThermalGuard.ThermalListener {
                override fun onThrottleRequired(bitrateScale: Float, fpsScale: Float) {
                    val newBitrate = (MediaConfig.BITRATE_8MBPS * bitrateScale).toInt()
                    sourceManager?.updateBitrate(newBitrate)
                }
            })
        }
    }

    private fun setupMirroring(resultCode: Int, resultData: Intent, destinationIp: String) {
        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpManager.getMediaProjection(resultCode, resultData)

        val metrics = DisplayMetrics()
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getRealMetrics(metrics)

        // Initialize modular source manager
        sourceManager = MirrorSourceManager(serviceScope, metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
        
        if (mediaProjection == null) {
            Log.e("MirrorService", "Phase 3 Failure: MediaProjection is null. Stopping service.")
            stopSelf()
            return
        }

        mediaProjection?.let {
            sourceManager?.start(it, destinationIp)
        }
    }

    private fun initiateSelfHealing() {
        Log.d("MirrorService", "Initiating Phase 5 Self-Healing...")
        cleanupResources()
        // Here we could notify the UI or try to restart if we had the intent data
    }

    private fun startControlChannel() {
        controlJob?.cancel()
        controlJob = serviceScope.launch(Dispatchers.IO) {
            val transport = UdpTransport(destinationIp, NetworkConfig.CONTROL_PORT)
            try {
                transport.open(NetworkConfig.CONTROL_PORT)
                val buffer = ByteArray(2048)
                while (isActive) {
                    val len = transport.receive(buffer)
                    if (len > 0) {
                        // Background State Learning
                        transport.getLastSenderAddress()?.let { senderIp ->
                            if (destinationIp != senderIp) {
                                destinationIp = senderIp
                            }
                        }
                        
                        protocolManager?.handleIncomingPacket(
                            data = buffer.copyOf(len),
                            onStateChanged = { state -> 
                                // Broadcast sync to UI via Bus
                                serviceScope.launch {
                                    SyncStateBus.publishState(state)
                                }
                                Log.d("MirrorService", "Background Sync: ${state.appState}")
                            },
                            onControlReceived = { vector ->
                                // Phase 9: Self-Injection in Background
                                serviceScope.launch {
                                    touchInjector.injectTouch(vector.x, vector.y, vector.isClicked)
                                    SyncStateBus.publishControl(vector) // Relay to UI for dot/feedback
                                }
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MirrorService", "Control error: ${e.message}")
            } finally {
                transport.close()
            }
        }
    }

    private fun startForegroundService(subtext: String = "Active") {
        val channelId = "mirror_service_channel"
        val channel = NotificationChannel(channelId, "Mirroring Service", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Connect Service")
            .setContentText(subtext)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
            
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(1, notification)
        }
    }

    private fun cleanupResources() {
        Log.d("MirrorService", "Modular Cleanup Sequence Initiated")
        sourceManager?.stop()
        mediaProjection?.stop()
        
        sourceManager = null
        mediaProjection = null
    }

    private fun cleanupFunctions() {
        sourceManager?.stop()
        sourceManager = null
        // Note: Sink (MediaDecoder) is managed by ViewModel for now, but service stays alive for sync.
        Log.i("MirrorService", "Internal functions cleaned up, but service remains alive for sync.")
    }

    override fun onDestroy() {
        serviceScope.cancel()
        cleanupResources()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}
