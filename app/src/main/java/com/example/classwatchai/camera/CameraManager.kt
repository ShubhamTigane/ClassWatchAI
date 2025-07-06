package com.example.classwatchai.camera

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.classwatchai.detection.GestureDetector
import com.example.classwatchai.ui.adapters.OverlayView
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object CameraManager {

    private var lifecycleOwner: LifecycleOwner? = null
    private var previewView: PreviewView? = null
    private var overlay: OverlayView? = null

    private lateinit var appContext: Context
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()


    private const val TAG = "Myres CameraManager"

    fun initialize(
        context : Context,
        owner   : LifecycleOwner? = null,
        preview : PreviewView?    = null
    ) {
        appContext      = context.applicationContext
        lifecycleOwner  = owner ?: lifecycleOwner
        previewView     = preview ?: previewView
        // When previewView changes we update preview‑size hint for detector
        previewView?.scaleType = PreviewView.ScaleType.FILL_CENTER

        previewView?.post {
            GestureDetector.setPreviewSize(
                previewView!!.width.toFloat(),
                previewView!!.height.toFloat()
            )
        }
    }
    fun setOverlay(view: OverlayView) {
        overlay = view
        GestureDetector.attachOverlay(view)
    }

    fun startCamera() {
        Log.d(TAG, "startCamera()")

        val owner = lifecycleOwner
        val preview = previewView

        val analysisUseCase = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(cameraExecutor, FrameAnalyzer())
            }

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        val cameraProviderFuture = ProcessCameraProvider.getInstance(appContext)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            cameraProvider?.unbindAll()

            if (owner != null && preview != null) {
                val previewUseCase = Preview.Builder().build().apply {
                    setSurfaceProvider(preview.surfaceProvider)
                }
                cameraProvider?.bindToLifecycle(owner, cameraSelector, previewUseCase, analysisUseCase)
                Log.d(TAG, "✅ Camera bound to lifecycle (with preview)")
            } else {
                val dummyOwner = DummyLifecycleOwner()
                cameraProvider?.bindToLifecycle(dummyOwner, cameraSelector, analysisUseCase)
                Log.d(TAG, "✅ Camera bound in headless mode")
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        Log.d(TAG, "🛑 Camera stopped")
    }

    private class FrameAnalyzer : ImageAnalysis.Analyzer {
        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(image: ImageProxy) {
            try {
                GestureDetector.processFrame(appContext, image)
            } catch (e: Exception) {
                Log.e(TAG, "myres Error in analyzer", e)
            } finally {
                image.close()
            }
        }
    }
    // ───────── CameraManager.kt (only this tiny change) ─────────
    fun pushLandmarks(
        facePts : List<Pair<Float, Float>>,
        labels  : List<String>,                   // <‑‑ NEW
        handPts : List<Pair<Float, Float>>
    ) {
        overlay?.showLandmarks(facePts, labels, handPts)
    }


}
