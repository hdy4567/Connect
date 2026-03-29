package com.example.connect

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.connect.ui.theme.ConnectTheme
import com.example.connect.ui.viewmodel.*
import com.example.connect.ui.screens.*
import com.example.connect.mirror.MirrorService
import com.example.connect.mirror.HidDataSender
import com.example.connect.util.PermissionManager
import com.example.connect.ui.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var permissionManager: PermissionManager
    private var activityViewModel: ConnectViewModel? = null

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            startMirrorService(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "Screen mirroring permission denied", Toast.LENGTH_SHORT).show()
            activityViewModel?.setMirroringActive(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        
        permissionManager = PermissionManager(this) {
            activityViewModel?.setPermissionGranted(true)
        }

        val prefs = getSharedPreferences("connect_prefs", Context.MODE_PRIVATE)
        val initialPermission = prefs.getBoolean("permissions_granted", false)

        setContent {
            var permissionGranted by remember { mutableStateOf(initialPermission) }

            ConnectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF050505)
                ) {
                    if (!permissionGranted) {
                        PermissionScreen(
                            onGrantClick = { 
                                permissionManager.startPermissionChain()
                                prefs.edit().putBoolean("permissions_granted", true).apply()
                                permissionGranted = true
                            }
                        )
                    } else {
                        MainContent()
                    }
                }
            }
        }
    }

    @Composable
    private fun MainContent() {
        val vm: ConnectViewModel = viewModel(
            factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )
        this.activityViewModel = vm
        
        // Initialize HidDataSender and WifiDirect
        LaunchedEffect(Unit) {
            vm.initHidDataSender(HidDataSender(this@MainActivity))
            vm.setWifiDirectManager(com.example.connect.util.WifiDirectManager(this@MainActivity, object : com.example.connect.util.OnConnectionListener {
                override fun onDiscoveryStarted() {}
                override fun onDiscoveryFailed(reason: Int) {}
                override fun onPeerFound(device: android.net.wifi.p2p.WifiP2pDevice) {
                    vm.onPeerFound(device)
                }
                override fun onConnectionReady(goAddress: String) {
                    vm.onConnectionReady(goAddress)
                }
                override fun onDisconnected() {
                    vm.onDisconnected()
                }
            }).also { it.register(this@MainActivity) })
        }
        
        AppNavigation(
            viewModel = vm,
            onStartProjection = {
                projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
            },
            onMinimize = {
                moveTaskToBack(true)
            }
        )
    }

    private fun startMirrorService(resultCode: Int, data: Intent) {
        val vm = this.activityViewModel ?: return
        
        val serviceIntent = Intent(this, MirrorService::class.java).apply {
            putExtra("RESULT_CODE", resultCode)
            putExtra("RESULT_DATA", data)
            putExtra("DESTINATION_IP", vm.destinationIp)
        }
        
        try {
            startForegroundService(serviceIntent)
            vm.setMirroringActive(true)
            Log.d("MainActivity", "MirrorService started successfully.")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start MirrorService: ${e.message}", e)
            Toast.makeText(this, "Failed to start mirroring: ${e.message}", Toast.LENGTH_LONG).show()
            vm.setMirroringActive(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
