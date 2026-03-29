package com.example.connect.util

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import com.example.connect.config.NetworkConfig

/**
 * WifiDirectManager handles P2P discovery, group formation, and connection events.
 * It maintains the performance constraints (harness) required for low-latency streaming.
 */
class WifiDirectManager(
    private val context: Context,
    private val listener: OnConnectionListener
) {
    private val manager: WifiP2pManager? by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    }
    private val channel: WifiP2pManager.Channel? by lazy {
        manager?.initialize(context, context.mainLooper, null)
    }

    private var receiver: BroadcastReceiver? = null

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        Log.i("WifiDirectManager", "Initiating Hybrid Discovery (DNS-SD + Legacy Scan)")
        
        // 1. Clear stale groups first to free the radio
        val currentChannel = channel
        if (manager == null || currentChannel == null) {
            Log.e("WifiDirectManager", "Cannot start discovery: Manager or Channel is null")
            return
        }

        try {
            manager?.removeGroup(currentChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() { Log.d("WifiDirectManager", "Previous group cleared") }
                override fun onFailure(reason: Int) { /* Might fail if no group exists, ignore */ }
            })
        } catch (e: Exception) {
            Log.e("WifiDirectManager", "Error clearing group: ${e.message}")
        }

        // 2. Prepare Phase 2: Zero-Config Service Discovery (DNS-SD)
        val uniqueInstance = "Connect_${android.os.Build.MODEL.take(5)}_${(100..999).random()}"
        val serviceInfo = mapOf("name" to uniqueInstance, "port" to NetworkConfig.DEFAULT_DEST_PORT.toString())
        val serviceRecord = android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo.newInstance(
            uniqueInstance, "_connect_mirror._udp", serviceInfo
        )

        manager?.clearLocalServices(channel, null)
        manager?.clearServiceRequests(channel, null)
        
        try {
            manager?.addLocalService(currentChannel, serviceRecord, object : WifiP2pManager.ActionListener {
                override fun onSuccess() { Log.d("WifiDirectManager", "Local DNS-SD service added ($uniqueInstance)") }
                override fun onFailure(reason: Int) { Log.e("WifiDirectManager", "Local service failed: $reason") }
            })
        } catch (e: Exception) {
            Log.e("WifiDirectManager", "Error adding local service: ${e.message}")
        }

        manager?.setDnsSdResponseListeners(channel, { instanceName, serviceType, device ->
            Log.d("WifiDirectManager", "DNS-SD Discovered: $instanceName ($serviceType)")
            if (instanceName.startsWith("Connect") || serviceType.contains("_connect_mirror")) {
                Log.i("WifiDirectManager", "🎯 Connect Service Found: ${device.deviceName} @ ${device.deviceAddress}")
                listener.onPeerFound(device)
            }
        }, { fullDomainName, txtRecordMap, device ->
            Log.v("WifiDirectManager", "TXT Record: $txtRecordMap")
        })

        val serviceRequest = android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest.newInstance()
        manager?.addServiceRequest(channel, serviceRequest, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                manager?.discoverServices(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() { Log.d("WifiDirectManager", "Service discovery started") }
                    override fun onFailure(reason: Int) { Log.e("WifiDirectManager", "Service discovery failed: $reason") }
                })
            }
            override fun onFailure(reason: Int) { Log.e("WifiDirectManager", "Service request failed: $reason") }
        })

        // 3. Fallback: Also start legacy peer discovery
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { Log.d("WifiDirectManager", "Legacy peer scan started") }
            override fun onFailure(reason: Int) { Log.e("WifiDirectManager", "Legacy scan failed: $reason") }
        })
    }

    fun register(context: Context) {
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        receiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                        val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                        if (state == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {
                            listener.onDisconnected()
                        }
                    }
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        // Legacy scanning fallback
                        val currentChannel = channel
                        if (currentChannel != null) {
                            try {
                                manager?.requestPeers(currentChannel) { peers ->
                                    Log.v("WifiDirectManager", "Peers updated: ${peers?.deviceList?.size ?: 0} found")
                                    peers?.deviceList?.forEach { device ->
                                        listener.onPeerFound(device)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("WifiDirectManager", "Error requesting peers: ${e.message}")
                            }
                        }
                    }
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        val networkInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO, NetworkInfo::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                        }
                        if (networkInfo?.isConnected == true) {
                            manager?.requestConnectionInfo(channel) { info ->
                                if (info.groupFormed) {
                                    val goAddress = info.groupOwnerAddress?.hostAddress ?: NetworkConfig.DEFAULT_DEST_ADDRESS
                                    listener.onConnectionReady(goAddress)
                                }
                            }
                        } else {
                            // Only notify disconnected if we were expecting to be connected
                            // listener.onDisconnected() // Suppress for now to avoid loops in MirrorService
                        }
                    }
                }
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, intentFilter)
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: WifiP2pDevice, groupOwnerIntent: Int = 0) {
        val config = android.net.wifi.p2p.WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            this.groupOwnerIntent = groupOwnerIntent // 0: client, 15: GO
        }
        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("WifiDirectManager", "Connection attempt initiated for ${device.deviceName} with intent $groupOwnerIntent")
            }
            override fun onFailure(reason: Int) {
                Log.e("WifiDirectManager", "Connection attempt failed: $reason")
            }
        })
    }

    fun unregister(context: Context) {
        receiver?.let { context.unregisterReceiver(it) }
        receiver = null
    }
}
