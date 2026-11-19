package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.model.ListingWithMeta
import com.example.data.model.formatInr
import com.example.data.util.openGoogleMapsDirections
import com.example.ui.theme.HarvestForest
import com.example.ui.theme.TerracottaClay
import com.example.ui.theme.TransitBlue
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.cos
import kotlin.math.roundToInt

fun isGoogleMapsApiKeyConfigured(): Boolean {
    val key = BuildConfig.MAPS_API_KEY
    return key.isNotBlank() && key != "MY_MAPS_API_KEY" && !key.startsWith("MY_")
}

/**
 * Interactive nearby farm discovery map powered by Google Maps Compose SDK when MAPS_API_KEY
 * is provided in AI Studio Secrets, with an interactive Canvas Proximity Radar fallback
 * so the app stays visually crisp and functional even before a key is entered.
 */
@Composable
fun NearbyFarmsMapCard(
    userLat: Double,
    userLng: Double,
    radiusKm: Double?,
    listings: List<ListingWithMeta>,
    onSelectListing: (ListingWithMeta) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasLiveKey = remember { isGoogleMapsApiKeyConfigured() }
    var focusedItem by remember(listings) { mutableStateOf(listings.firstOrNull()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .testTag("nearby_farms_map_container")
            ) {
                if (hasLiveKey) {
                    val center = LatLng(userLat, userLng)
                    val cameraState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(center, 11.5f)
                    }
                    LaunchedEffect(userLat, userLng, radiusKm) {
                        val zoom = when {
                            radiusKm == null -> 9f
                            radiusKm <= 10.0 -> 12.2f
                            radiusKm <= 25.0 -> 11f
                            else -> 9.8f
                        }
                        cameraState.animate(CameraUpdateFactory.newLatLngZoom(center, zoom))
                    }
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraState,
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            myLocationButtonEnabled = false
                        )
                    ) {
                        Marker(
                            state = MarkerState(position = center),
                            title = "Your Location",
                            snippet = "Searching within ${radiusKm?.toInt() ?: 100} km",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )
                        if (radiusKm != null) {
                            Circle(
                                center = center,
                                radius = radiusKm * 1000.0,
                                fillColor = HarvestForest.copy(alpha = 0.12f),
                                strokeColor = HarvestForest.copy(alpha = 0.65f),
                                strokeWidth = 3f
                            )
                        }
                        listings.forEach { item ->
                            Marker(
                                state = MarkerState(
                                    position = LatLng(item.listing.latitude, item.listing.longitude)
                                ),
                                title = "${item.listing.cropName} · ${formatInr(item.listing.pricePerKg)}/kg",
                                snippet = "${item.listing.farmName} (${item.distanceKm} km)",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                                onClick = {
                                    focusedItem = item
                                    false
                                }
                            )
                        }
                    }
                } else {
                    InteractiveFarmProximityRadar(
                        userLat = userLat,
                        userLng = userLng,
                        radiusKm = radiusKm ?: 50.0,
                        listings = listings,
                        focusedId = focusedItem?.listing?.id,
                        onPinTapped = { focusedItem = it }
                    )
                }

                // Top-left live radius pill
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Map,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (radiusKm != null) {
                                "${listings.size} farms within ${radiusKm.toInt()} km"
                            } else {
                                "${listings.size} farms across region"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (!hasLiveKey) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = stringResource(R.string.maps_key_banner),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            focusedItem?.let { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectListing(item) }
                        .padding(14.dp)
                        .testTag("map_selected_farm_card"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${item.listing.cropName} · ${formatInr(item.listing.pricePerKg)}/kg",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.listing.farmName} · ${item.distanceKm} km away",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                    FilledTonalButton(
                        onClick = {
                            openGoogleMapsDirections(
                                context = context,
                                destLat = item.listing.latitude,
                                destLng = item.listing.longitude,
                                label = item.listing.farmName
                            )
                        },
                        modifier = Modifier.testTag("map_open_directions_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Directions,
                            contentDescription = "Navigate to farm",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(4.dp))
                        Text("Route")
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveFarmProximityRadar(
    userLat: Double,
    userLng: Double,
    radiusKm: Double,
    listings: List<ListingWithMeta>,
    focusedId: String?,
    onPinTapped: (ListingWithMeta) -> Unit
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val bgTop = Color(0xFFEAF4EC)
    val bgBottom = Color(0xFFDCEBE0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
            .onSizeChanged { boxSize = it }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = minOf(size.width, size.height) * 0.42f
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

            // Subtle geographic grid lines
            val gridStep = size.width / 6f
            for (i in 1..5) {
                val x = gridStep * i
                drawLine(
                    color = primaryColor.copy(alpha = 0.08f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.5f
                )
            }
            val rowStep = size.height / 4f
            for (j in 1..3) {
                val y = rowStep * j
                drawLine(
                    color = primaryColor.copy(alpha = 0.08f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5f
                )
            }

            // Concentric proximity rings
            listOf(0.38f, 0.70f, 1.0f).forEach { fraction ->
                drawCircle(
                    color = primaryColor.copy(alpha = if (fraction == 1f) 0.11f else 0.05f),
                    radius = maxRadius * fraction,
                    center = center
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.35f),
                    radius = maxRadius * fraction,
                    center = center,
                    style = Stroke(width = 2f, pathEffect = if (fraction < 1f) dashEffect else null)
                )
            }
        }

        // User GPS pin at center
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(32.dp)
                .clip(CircleShape)
                .background(TransitBlue.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(TransitBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = "Your GPS position",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        // Plot farm pins proportionally within the radar radius
        if (boxSize.width > 0 && boxSize.height > 0) {
            val centerX = boxSize.width / 2f
            val centerY = boxSize.height / 2f
            val maxR = minOf(boxSize.width, boxSize.height) * 0.39f
            val kmPerDegLat = 111.0
            val kmPerDegLng = (111.0 * cos(Math.toRadians(userLat))).coerceAtLeast(20.0)
            val pinSizePx = with(density) { 40.dp.toPx() }

            listings.take(12).forEachIndexed { index, item ->
                val dyKm = (item.listing.latitude - userLat) * kmPerDegLat
                val dxKm = (item.listing.longitude - userLng) * kmPerDegLng
                val clampedScale = radiusKm.coerceAtLeast(5.0)
                var normX = (dxKm / clampedScale).coerceIn(-0.92, 0.92).toFloat()
                var normY = (-dyKm / clampedScale).coerceIn(-0.92, 0.92).toFloat()

                // Slight fan-out offset when multiple harvests share identical coordinates
                if (normX in -0.05f..0.05f && normY in -0.05f..0.05f) {
                    val angle = (index * 1.15)
                    normX = (kotlin.math.cos(angle) * 0.30).toFloat()
                    normY = (kotlin.math.sin(angle) * 0.30).toFloat()
                }

                val pxX = (centerX + normX * maxR - pinSizePx / 2f).roundToInt()
                val pxY = (centerY + normY * maxR - pinSizePx / 2f).roundToInt()
                val isSelected = item.listing.id == focusedId

                Surface(
                    modifier = Modifier
                        .offset { IntOffset(pxX, pxY) }
                        .size(if (isSelected) 44.dp else 38.dp)
                        .clickable { onPinTapped(item) }
                        .testTag("radar_farm_pin_${item.listing.id}"),
                    shape = CircleShape,
                    color = if (isSelected) TerracottaClay else HarvestForest,
                    shadowElevation = if (isSelected) 6.dp else 3.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Agriculture,
                            contentDescription = item.listing.farmName,
                            tint = Color.White,
                            modifier = Modifier.size(if (isSelected) 22.dp else 18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact route map preview for Delivery Drivers showing Farm Pickup -> Consumer Drop-off.
 */
@Composable
fun DeliveryRouteMapPreview(
    pickupLat: Double,
    pickupLng: Double,
    pickupLabel: String,
    dropLat: Double,
    dropLng: Double,
    dropLabel: String,
    distanceKm: Double,
    modifier: Modifier = Modifier
) {
    val hasLiveKey = remember { isGoogleMapsApiKeyConfigured() }
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(MaterialTheme.shapes.medium)
    ) {
        if (hasLiveKey) {
            val midLat = (pickupLat + dropLat) / 2.0
            val midLng = (pickupLng + dropLng) / 2.0
            val pickup = LatLng(pickupLat, pickupLng)
            val drop = LatLng(dropLat, dropLng)
            val cameraState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(LatLng(midLat, midLng), 11.5f)
            }
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    scrollGesturesEnabled = false,
                    zoomGesturesEnabled = false,
                    tiltGesturesEnabled = false
                )
            ) {
                Marker(
                    state = MarkerState(position = pickup),
                    title = "Pickup: $pickupLabel",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                )
                Marker(
                    state = MarkerState(position = drop),
                    title = "Drop-off: $dropLabel",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                )
                Polyline(
                    points = listOf(pickup, drop),
                    color = HarvestForest,
                    width = 8f
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFE7F3EA), Color(0xFFF9EFE4))
                        )
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val start = Offset(size.width * 0.16f, size.height * 0.56f)
                    val end = Offset(size.width * 0.84f, size.height * 0.44f)
                    drawLine(
                        color = primaryColor,
                        start = start,
                        end = end,
                        strokeWidth = 6f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RouteEndpointChip(
                        icon = Icons.Filled.Agriculture,
                        label = "Pickup",
                        detail = pickupLabel,
                        tint = HarvestForest
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = "${distanceKm} km",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    RouteEndpointChip(
                        icon = Icons.Filled.LocationOn,
                        label = "Drop-off",
                        detail = dropLabel,
                        tint = TerracottaClay
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteEndpointChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    detail: String,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = tint
        )
        Text(
            text = detail.take(18),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
