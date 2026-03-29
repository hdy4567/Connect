package com.example.connect.mirror

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import com.example.connect.config.NetworkConfig

/**
 * Handles high-speed UDP packet transmission for video streams.
 */
class UdpTransport(
    private val address: String = NetworkConfig.DEFAULT_DEST_ADDRESS,
    private val port: Int = NetworkConfig.DEFAULT_DEST_PORT
) {
    private var socket: DatagramSocket? = null
    private var inetAddress: InetAddress? = null

    fun open(bindPort: Int? = null) {
        socket = if (bindPort != null) {
            DatagramSocket(bindPort)
        } else {
            DatagramSocket()
        }
        socket?.soTimeout = 5000 // 5s timeout to prevent blocking lag
        inetAddress = InetAddress.getByName(address)
    }

    fun send(data: ByteArray) {
        try {
            val packet = DatagramPacket(data, data.size, inetAddress, port)
            socket?.let {
                if (!it.isClosed) it.send(packet)
            }
        } catch (e: Exception) {
            android.util.Log.e("UdpTransport", "Send error: ${e.message}")
        }
    }

    private var lastSenderAddress: String? = null
    fun getLastSenderAddress(): String? = lastSenderAddress

    fun receive(buffer: ByteArray): Int {
        return try {
            val packet = DatagramPacket(buffer, buffer.size)
            socket?.let {
                if (!it.isClosed) {
                    it.receive(packet)
                    lastSenderAddress = packet.address.hostAddress
                    packet.length
                } else -1
            } ?: -1
        } catch (e: Exception) {
            if (socket?.isClosed != true) {
                android.util.Log.e("UdpTransport", "Receive error: ${e.message}")
            }
            -1
        }
    }

    fun close() {
        socket?.close()
        socket = null
    }
}
