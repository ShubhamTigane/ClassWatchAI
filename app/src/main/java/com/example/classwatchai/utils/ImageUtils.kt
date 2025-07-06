package com.example.classwatchai.utils

import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilities for converting / saving frames produced by CameraX.
 *
 * ⚠️  Design goals:
 * • Never access an ImageProxy *after* it has been closed.
 * • Do **one** YUV → JPEG conversion, then work only with Bitmaps/byte‑arrays.
 */
object ImageUtils {

    /** ------------------------------------------------------------------ */
    /** 1️⃣  Convert ImageProxy (YUV_420_888) to a mutable Bitmap.          */
    /** ------------------------------------------------------------------ */
    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? = try {
        // ── ①  YUV → JPEG in memory
        val yuvImage = imageProxy.toYuvImage() ?: return null
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            Rect(0, 0, imageProxy.width, imageProxy.height),
            100,
            out
        )
        val jpegBytes = out.toByteArray()

        // ── ②  Decode JPEG → Bitmap
        BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
            ?.copy(Bitmap.Config.ARGB_8888, /*mutable=*/true)
    } catch (e: Exception) {
        Log.e("ImageUtils", "❌ imageProxyToBitmap: ${e.message}", e)
        null
    }

    /** ------------------------------------------------------------------ */
    /** 2️⃣  Save a Bitmap to a file, return absolute path.                 */
    /** ------------------------------------------------------------------ */
    fun saveBitmapToFile(
        context: Context,
        bitmap: Bitmap,
        rotateDegrees: Int = 0,
        quality: Int = 90,
        prefix: String = "gesture"
    ): String {
        // Optional rotation
        val rotated = if (rotateDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotateDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }

        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val fileName = "${prefix}_$timeStamp.jpg"
        val file = File(context.getExternalFilesDir(null), fileName)

        FileOutputStream(file).use { out ->
            rotated.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }

        Log.d("ImageUtils", "💾 Saved frame → ${file.absolutePath}")
        return file.absolutePath
    }

    /** ------------------------------------------------------------------ */
    /** 🔒 INTERNAL : Convert ImageProxy planes → YuvImage (NV21).          */
    /** ------------------------------------------------------------------ */
    private fun ImageProxy.toYuvImage(): YuvImage? = try {
        val yBuf = planes[0].buffer
        val uBuf = planes[1].buffer
        val vBuf = planes[2].buffer

        val ySize = yBuf.remaining()
        val uSize = uBuf.remaining()
        val vSize = vBuf.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuf.get(nv21, 0, ySize)
        vBuf.get(nv21, ySize, vSize)          // V plane
        uBuf.get(nv21, ySize + vSize, uSize)  // U plane  (VU → NV21)

        YuvImage(nv21, ImageFormat.NV21, width, height, null)
    } catch (e: Exception) {
        Log.e("ImageUtils", "❌ toYuvImage: ${e.message}", e)
        null
    }
}
