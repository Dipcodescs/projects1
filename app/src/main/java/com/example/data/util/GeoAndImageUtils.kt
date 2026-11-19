package com.example.data.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class AgriculturalHub(
    val city: String,
    val state: String,
    val latitude: Double,
    val longitude: Double
)

val INDIAN_AGRI_HUBS = listOf(
    AgriculturalHub("Nashik", "Maharashtra", 19.9975, 73.7898),
    AgriculturalHub("Pune", "Maharashtra", 18.5204, 73.8567),
    AgriculturalHub("Bengaluru Rural", "Karnataka", 13.0827, 77.5946),
    AgriculturalHub("Ludhiana", "Punjab", 30.9010, 75.8573),
    AgriculturalHub("Coimbatore", "Tamil Nadu", 11.0168, 76.9558),
    AgriculturalHub("Indore", "Madhya Pradesh", 22.7196, 75.8577)
)

/**
 * Computes great-circle distance in kilometers using the Haversine formula,
 * matching PostGIS ST_DistanceSphere proximity queries on-device.
 */
fun haversineDistanceKm(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return ((earthRadiusKm * c) * 10.0).roundToInt() / 10.0
}

/**
 * Computes transparent delivery payout based on route distance and crop load weight.
 */
fun computeDriverDeliveryFee(distanceKm: Double, totalWeightKg: Double): Double {
    val base = 30.0
    val perKm = distanceKm.coerceAtLeast(1.0) * 6.0
    val weightExtra = if (totalWeightKg > 10.0) (totalWeightKg - 10.0) * 1.5 else 0.0
    return ((base + perKm + weightExtra) * 10.0).roundToInt() / 10.0
}

data class CompressedImageResult(
    val base64: String,
    val compressedKb: Int,
    val width: Int,
    val height: Int
)

/**
 * Client-side crop photo compression for low-bandwidth rural networks.
 * Downscales to max 320px and compresses JPEG until under ~45 KB (Base64 < 65,000 chars).
 */
suspend fun compressCropPhotoFromUri(context: Context, uri: Uri): CompressedImageResult? =
    withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            val rawWidth = boundsOptions.outWidth.coerceAtLeast(1)
            val rawHeight = boundsOptions.outHeight.coerceAtLeast(1)
            val maxDim = 320
            var sampleSize = 1
            while (rawWidth / sampleSize > maxDim * 2 || rawHeight / sampleSize > maxDim * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            val scale = minOf(maxDim.toFloat() / decoded.width, maxDim.toFloat() / decoded.height, 1f)
            val targetW = (decoded.width * scale).roundToInt().coerceAtLeast(1)
            val targetH = (decoded.height * scale).roundToInt().coerceAtLeast(1)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                    if (it != decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            var quality = 70
            var bytes: ByteArray
            do {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                bytes = out.toByteArray()
                quality -= 12
            } while (bytes.size > 46_000 && quality >= 28)

            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val width = scaled.width
            val height = scaled.height
            scaled.recycle()

            if (encoded.length > 74_000) return@withContext null
            CompressedImageResult(
                base64 = encoded,
                compressedKb = (bytes.size / 1024).coerceAtLeast(1),
                width = width,
                height = height
            )
        } catch (_: Exception) {
            null
        }
    }

fun decodeBase64Bitmap(base64: String?): Bitmap? {
    if (base64.isNullOrBlank()) return null
    return try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}

/**
 * Opens Google Maps turn-by-turn navigation or location pin using standard Android Intent.
 */
fun openGoogleMapsDirections(
    context: Context,
    destLat: Double,
    destLng: Double,
    label: String
) {
    val encodedLabel = Uri.encode(label)
    val navUri = Uri.parse("google.navigation:q=$destLat,$destLng")
    val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    val geoFallback = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("geo:$destLat,$destLng?q=$destLat,$destLng($encodedLabel)")
    )
    try {
        if (navIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(navIntent)
        } else {
            context.startActivity(geoFallback)
        }
    } catch (_: Exception) {
        try {
            val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destLat,$destLng")
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        } catch (_: Exception) {
            // No browser or maps handler available
        }
    }
}
