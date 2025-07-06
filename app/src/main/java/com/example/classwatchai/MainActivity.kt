package com.example.classwatchai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.classwatchai.camera.CameraManager
import com.example.classwatchai.data.db.LocalGestureLogger
import com.example.classwatchai.detection.GestureDetector
import com.example.classwatchai.service.MonitoringService
import com.example.classwatchai.ui.adapters.GestureLogAdapter
import com.example.classwatchai.ui.adapters.OverlayView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnToggleCamera: Button
    private lateinit var adapter: GestureLogAdapter
    lateinit var overlay: OverlayView

    private var isMonitoring = false

    private val cameraPermission = Manifest.permission.CAMERA

    private var refreshJob: Job? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate()")
        setContentView(R.layout.activity_main)

        // Initialize Room DB
        LocalGestureLogger.init(applicationContext)
        Log.d(TAG, "Room DB initialized")

        // Setup UI
        previewView = findViewById(R.id.previewView)
        // ✅ Set preview scaling type
      //  previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

        recyclerView = findViewById(R.id.recyclerViewLogs)
        btnToggleCamera = findViewById(R.id.btnToggleCamera)
        overlay = findViewById(R.id.overlay)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = GestureLogAdapter(emptyList())
        recyclerView.adapter = adapter

        CameraManager.initialize(applicationContext, this , previewView)   // with PreviewView
        CameraManager.setOverlay(overlay)
        CameraManager.startCamera()

        btnToggleCamera.setOnClickListener {
            toggleMonitoring()
        }

        val btnClear = findViewById<Button>(R.id.btnClearLogs)
        btnClear.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                LocalGestureLogger.clearAll()
                Log.i("Myres DB", "🧹 Gesture logs cleared")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Logs cleared", Toast.LENGTH_SHORT).show()
                }
            }
        }


        requestAllPermissions()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun toggleMonitoring() {
        if (isMonitoring) stopMonitoring() else startMonitoring()
    }


    override fun onResume() {
        super.onResume()
       // loadGestureLogs()
        startAutoRefresh()
    }
    override fun onPause() {
        super.onPause()
        refreshJob?.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startPreview() {
        previewView.post {
               Log.d(TAG, "🔸 Ensuring camera preview starts after layout")
            CameraManager.startCamera()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestAllPermissions() {
        val requiredPermissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requiredPermissions.add(Manifest.permission.FOREGROUND_SERVICE_CAMERA)
        }

        val notGranted = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            startPreview()
            Log.d(TAG, "Requesting permissions: $notGranted")
            permissionLauncher.launch(notGranted.toTypedArray())
        } else {
            Log.d(TAG, "✅ All permissions already granted")
            startPreview() // <-- make sure preview starts even when permissions are already granted
            btnToggleCamera.isEnabled = true
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.all { it.value }
        if (allGranted) {
            Log.d(TAG, "✅ Permission result: $result")
            startPreview()
            btnToggleCamera.isEnabled = true
        } else {
            Log.e("MainActivity", "🔴Required permissions were denied.")
            btnToggleCamera.isEnabled = false
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startMonitoring() {
        Log.d(TAG, "startMonitoring(): launching service only")
        GestureDetector.startMonitoring()            // NEW 🟢 begin analysing frames

        val svc = Intent(this, MonitoringService::class.java)
        ContextCompat.startForegroundService(this, svc)
        btnToggleCamera.text = "Stop Monitoring"
        isMonitoring = true
    }

    private fun stopMonitoring() {
        Log.d(TAG, "stopMonitoring(): stopping service only")
        @Suppress("UsePropertyAccessSyntax")
        GestureDetector.stopMonitoring()             // NEW 🔴 halt analysing frames
        stopService(Intent(this, MonitoringService::class.java))
        btnToggleCamera.text = "Start Monitoring"
        isMonitoring = false
    }


    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                val logs = LocalGestureLogger.getAllLogs()
                adapter.updateLogs(logs)
                delay(3_000) // refresh every 3 s
            }
        }
    }

    companion object { private const val TAG = "Myres MainActivity" }
}
