package com.example.connect.util

import android.net.wifi.p2p.WifiP2pDevice

/**
 * Interface for Wi-Fi Direct connection events.
 */
interface OnConnectionListener {
    fun onDiscoveryStarted()
    fun onDiscoveryFailed(reason: Int)
    fun onPeerFound(device: WifiP2pDevice)
    fun onConnectionReady(goAddress: String)
    fun onDisconnected()
}
