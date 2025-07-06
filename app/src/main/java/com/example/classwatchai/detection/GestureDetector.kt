package com.example.classwatchai.detection

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.example.classwatchai.camera.CameraManager
import com.example.classwatchai.data.db.GestureLog
import com.example.classwatchai.data.db.LocalGestureLogger
import com.example.classwatchai.ui.adapters.OverlayView
import com.example.classwatchai.utils.ImageUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.graphics.RectF


object GestureDetector {

    /* ------------------------------------------------------------------ */
    /*  🔌  Hooks injected from CameraManager                             */
    /* ------------------------------------------------------------------ */
    private var overlay : OverlayView? = null
    private var previewW = 0f      // size of PreviewView on screen
    private var previewH = 0f

    /** Called once from CameraManager.setOverlay(..) */
    fun attachOverlay(v: OverlayView) { overlay = v }

    /** Called from CameraManager.initialize(..) */
    fun setPreviewSize(w: Float, h: Float) { previewW = w; previewH = h }

    /* ------------------------------------------------------------------ */
    /*  Monitoring switch                                                 */
    /* ------------------------------------------------------------------ */
    private var isMonitoring = false

    fun startMonitoring() { isMonitoring = true;  Log.d(TAG, "🟢 Monitoring started") }
    fun stopMonitoring () { isMonitoring = false; Log.d(TAG, "🔴 Monitoring stopped") }
    fun isMonitoringActive(): Boolean = isMonitoring

    /* ------------------------------------------------------------------ */
    /*  ML‑Kit Face detector                                              */
    /* ------------------------------------------------------------------ */
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)  // ← needed for dots
            .enableTracking()
            .build()
    )

    /* ------------------------------------------------------------------ */
    /*  Per‑frame processing                                              */
    /* ------------------------------------------------------------------ */
    private const val TAG = "Myres GestureDetector"
    @OptIn(ExperimentalGetImage::class)
    fun processFrame(ctx: Context, proxy: ImageProxy) {

        if (!isMonitoring) { proxy.close(); return }

        /* 1️⃣ bitmap + rotation */
        val bmp = ImageUtils.imageProxyToBitmap(proxy) ?: run { proxy.close(); return }
        val rot = proxy.imageInfo.rotationDegrees                      // 0 / 90 / 180 / 270
        val image = InputImage.fromBitmap(bmp, rot)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) Log.d(TAG, "🔍 No faces detected")

                for (face in faces) {

                    /* 2️⃣ map landmarks + labels */
                    val pts   = mutableListOf<Pair<Float,Float>>()
                    val labs  = mutableListOf<String>()

                    listOf(
                        FaceLandmark.LEFT_EYE     to "L‑Eye",
                        FaceLandmark.RIGHT_EYE    to "R‑Eye",
                        FaceLandmark.NOSE_BASE    to "Nose",
                        FaceLandmark.MOUTH_LEFT   to "M‑L",
                        FaceLandmark.MOUTH_RIGHT  to "M‑R",
                        FaceLandmark.MOUTH_BOTTOM to "M‑B"
                    ).forEach { (type, lbl) ->
                        face.getLandmark(type)?.position?.let { p ->
                            val (vx, vy) = mapToView(p.x, p.y, bmp.width, bmp.height, rot)
                            pts  += vx to vy
                            labs += lbl
                            Log.d("Landmark Mapping", "Original: (${p.x}, ${p.y}) → View: ($vx, $vy)")
                        }
                    }

                    /* 3️⃣ bounding box (same mapping) */
                    val bb = face.boundingBox
                    val (tlX, tlY) = mapToView(bb.left .toFloat(), bb.top   .toFloat(), bmp.width, bmp.height, rot)
                    val (brX, brY) = mapToView(bb.right.toFloat(), bb.bottom.toFloat(), bmp.width, bmp.height, rot)
                    val box = RectF(tlX, tlY, brX, brY)

                    /* 4️⃣ push to overlay */
                    overlay?.showLandmarks(pts, labs, emptyList(), box)

                    /* 5️⃣ gesture logic + DB logging (unchanged) */
                    detectGestureFromFace(face)?.let { g ->
                        if (GestureFilter.shouldLog(g)) {
                            val saved = ImageUtils.saveBitmapToFile(ctx, bmp, rot)
                            CoroutineScope(Dispatchers.IO).launch {
                                LocalGestureLogger.insert(
                                    GestureLog(
                                        timestamp   = g.timestamp,
                                        gesture_type= g.type,
                                        confidence  = g.confidence,
                                        duration    = g.duration,
                                        framePath   = saved,
                                        note        = g.note
                                    )
                                )
                                Log.d(TAG, "🟢 Logged: ${g.type}")
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { Log.e(TAG,"❌ Face detection failed", it) }
            .addOnCompleteListener { proxy.close() }
    }

    /* ───────────────────────── mapping helper ───────────────────────── */
    private fun mapToView(
        x: Float, y: Float,
        imgW: Int, imgH: Int,
        rotation: Int     // 0,90,180,270
    ): Pair<Float, Float> {

        /* 1️⃣ rotate into portrait‑upright space */
        val (rx, ry) = when (rotation) {
            0   -> x            to y
            90  -> y            to (imgW - x)
            180 -> (imgW - x)   to (imgH - y)
            /* 270 = your case (front cam) => CW rotate one more 90°  */
            else-> (imgH - y)   to x
        }

        /* 2️⃣ scale to PreviewView */
        val sx = previewW  / imgH.toFloat()     // note swapped
        val sy = previewH / imgW.toFloat()

        /* 3️⃣ mirror horizontally for front camera */
        val viewX = previewW - rx * sx          // mirror X
        val viewY =              ry * sy

        return viewX to viewY
    }

    /* ------------------------------------------------------------------ */
    /*  Simple gesture rules                                              */
    /* ------------------------------------------------------------------ */
    private fun detectGestureFromFace(face: Face): DetectedGesture? {
        val headY = face.headEulerAngleY
        val leftEye = face.leftEyeOpenProbability ?: 1f
        val rightEye = face.rightEyeOpenProbability ?: 1f
        val smile = face.smilingProbability ?: 0f
        val now = System.currentTimeMillis()

        return when {
            headY > 25 || headY < -25 ->
                DetectedGesture("LOOKING_AWAY", 0.8f, now)
            leftEye < 0.4f && rightEye < 0.4f ->
                DetectedGesture("EYES_CLOSED", 0.9f, now)
            smile > 0.85f ->
                DetectedGesture("DISTRACTED/TALKING", smile, now)
            else -> null
        }
    }
    private fun mapToViewCoordinates(
        x: Float, y: Float,
        imageWidth: Int, imageHeight: Int,
        viewWidth: Float, viewHeight: Float
    ): Pair<Float, Float> {
        // For ROTATION_270, the image is rotated CCW, so:
        // x' = y, y' = imageWidth - x
        val rotatedX = y
        val rotatedY = imageWidth - x

        // Now scale to previewView size
        val scaleX = viewWidth / imageHeight.toFloat()
        val scaleY = viewHeight / imageWidth.toFloat()

        val viewX = rotatedX * scaleX
        val viewY = rotatedY * scaleY

        // 🔁 Flip horizontally only if needed (usually for REAR camera)
        // For front camera, PreviewView already flips correctly
        Log.e(TAG, "Myres Rotation viewX=$viewX, viewY=$viewY")
        return viewX to viewY
    }

//    private fun mapToViewCoordinates(
//        x: Float, y: Float,
//        imageWidth: Int, imageHeight: Int,
//        viewWidth: Float, viewHeight: Float
//    ): Pair<Float, Float> {
//        val scaleX = viewWidth / imageHeight.toFloat()  // due to 270°
//        val scaleY = viewHeight / imageWidth.toFloat()
//
//        val rotatedX = y
//        val rotatedY = imageWidth - x
//
//        val viewX = rotatedX * scaleX
//        val viewY = rotatedY * scaleY
//        Log.e(TAG, "Myres Rotation viewX=$viewX, viewY=$viewY")
//
//        return viewX to viewY
//    }


//    private fun mapToViewCoordinates(
//        x: Float, y: Float,
//        imageWidth: Int, imageHeight: Int,
//        viewWidth: Float, viewHeight: Float
//    ): Pair<Float, Float> {
//        val scaleX = viewWidth / imageHeight.toFloat()  // swapped due to camera rotation
//        val scaleY = viewHeight / imageWidth.toFloat()
//
//
//        // If using front camera, flip horizontally
//        val viewX = viewWidth - y * scaleX
//        val viewY = x * scaleY
//
//        return viewX to viewY
//    }

}
