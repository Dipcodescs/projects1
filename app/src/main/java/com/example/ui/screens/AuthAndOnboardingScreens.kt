package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.UserRole
import com.example.data.util.INDIAN_AGRI_HUBS
import com.example.ui.i18n.AppLanguages
import com.example.ui.theme.HarvestForest
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    isBusy: Boolean,
    onCompleteOnboarding: (
        displayName: String,
        role: UserRole,
        phone: String,
        farmName: String,
        city: String,
        address: String,
        lat: Double,
        lng: Double,
        languageCode: String,
        vehicleType: String,
        vehiclePlate: String
    ) -> Unit
) {
    val context = LocalContext.current

    var selectedRole by remember { mutableStateOf(UserRole.CONSUMER) }
    var displayName by remember { mutableStateOf("Fresh Buyer") }
    var phone by remember { mutableStateOf("+91 ") }
    var farmName by remember { mutableStateOf("") }
    var city by remember { mutableStateOf(INDIAN_AGRI_HUBS.first().city) }
    var address by remember { mutableStateOf("Market Road, Nashik") }
    var latitude by remember { mutableDoubleStateOf(INDIAN_AGRI_HUBS.first().latitude) }
    var longitude by remember { mutableDoubleStateOf(INDIAN_AGRI_HUBS.first().longitude) }
    var languageCode by remember { mutableStateOf("en") }
    var vehicleType by remember { mutableStateOf("Mini Truck") }
    var vehiclePlate by remember { mutableStateOf("") }
    var gpsStatus by remember { mutableStateOf<String?>(null) }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchLastGpsLocation(
                context = context,
                onResult = { lat, lng ->
                    latitude = lat
                    longitude = lng
                    gpsStatus = String.format("GPS pinned: %.4f, %.4f", lat, lng)
                },
                onUnavailable = {
                    gpsStatus = "GPS unavailable in emulator — select an agricultural hub below."
                }
            )
        } else {
            gpsStatus = "Location permission denied — using selected regional hub."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Editorial harvest hero banner (direct entry — no Google Login required)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_harvest_hero),
                    contentDescription = "Fresh farm harvest marketplace",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    HarvestForest.copy(alpha = 0.35f),
                                    HarvestForest.copy(alpha = 0.78f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.auth_tagline),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "KisanDirect",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Choose your starting role — switch between Consumer, Farmer & Driver anytime.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Regional language picker
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Preferred Language", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguages.supported.forEach { code ->
                            val copy = AppLanguages.forCode(code)
                            FilterChip(
                                selected = languageCode == code,
                                onClick = { languageCode = code },
                                label = { Text(copy.nativeLabel) },
                                modifier = Modifier.testTag("onboarding_lang_$code")
                            )
                        }
                    }
                }

                // Role selector cards
                Text("Select Your Marketplace Role", style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoleOptionCard(
                        title = "Consumer / Household Buyer",
                        subtitle = "Browse nearby farms within 25 km, pay fair farm-gate prices & rate verified orders.",
                        icon = Icons.Filled.ShoppingBasket,
                        selected = selectedRole == UserRole.CONSUMER,
                        onClick = { selectedRole = UserRole.CONSUMER },
                        tag = "role_select_consumer"
                    )
                    RoleOptionCard(
                        title = "Farmer / Producer",
                        subtitle = "Publish fresh harvest batches directly with compressed crop photos & keep 96% payout.",
                        icon = Icons.Filled.Agriculture,
                        selected = selectedRole == UserRole.FARMER,
                        onClick = { selectedRole = UserRole.FARMER },
                        tag = "role_select_farmer"
                    )
                    RoleOptionCard(
                        title = "Delivery Driver",
                        subtitle = "Accept farm-to-door delivery tasks, track routes, update statuses & manage earnings.",
                        icon = Icons.Filled.LocalShipping,
                        selected = selectedRole == UserRole.DRIVER,
                        onClick = { selectedRole = UserRole.DRIVER },
                        tag = "role_select_driver"
                    )
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_name_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_phone_input")
                )

                if (selectedRole == UserRole.FARMER) {
                    OutlinedTextField(
                        value = farmName,
                        onValueChange = { farmName = it },
                        label = { Text("Farm / FPO Name") },
                        placeholder = { Text("e.g., Sahyadri Organic Farms") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_farm_name_input")
                    )
                }

                if (selectedRole == UserRole.DRIVER) {
                    Text("Vehicle Type", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Mini Truck", "Cargo Van", "E-Rickshaw", "Two-Wheeler").forEach { v ->
                            FilterChip(
                                selected = vehicleType == v,
                                onClick = { vehicleType = v },
                                label = { Text(v) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = vehiclePlate,
                        onValueChange = { vehiclePlate = it.uppercase() },
                        label = { Text("Vehicle Registration Plate") },
                        placeholder = { Text("e.g., MH15AB1234") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_vehicle_plate_input")
                    )
                }

                // Location / Hub picker
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Farm / Delivery Region", style = MaterialTheme.typography.titleSmall)
                        OutlinedButton(
                            onClick = {
                                locationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            },
                            modifier = Modifier.testTag("onboarding_gps_button")
                        ) {
                            Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Pin GPS")
                        }
                    }
                    if (gpsStatus != null) {
                        Text(
                            text = gpsStatus ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        INDIAN_AGRI_HUBS.forEach { hub ->
                            FilterChip(
                                selected = city == hub.city,
                                onClick = {
                                    city = hub.city
                                    latitude = hub.latitude
                                    longitude = hub.longitude
                                    if (address.isBlank() || address.startsWith("Market Road")) {
                                        address = "Market Road, ${hub.city}"
                                    }
                                },
                                label = { Text(hub.city) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = {
                            Text(
                                if (selectedRole == UserRole.FARMER) "Farm Gate Pickup Address"
                                else "Default Delivery Address"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = {
                        val resolvedFarm = if (selectedRole == UserRole.FARMER && farmName.isBlank()) {
                            "${displayName.trim()}'s Farm"
                        } else {
                            farmName.trim()
                        }
                        onCompleteOnboarding(
                            displayName.trim().ifBlank { "Kisan User" },
                            selectedRole,
                            phone.trim(),
                            resolvedFarm,
                            city,
                            address.trim().ifBlank { city },
                            latitude,
                            longitude,
                            languageCode,
                            if (selectedRole == UserRole.DRIVER) vehicleType else "",
                            if (selectedRole == UserRole.DRIVER) vehiclePlate.trim().ifBlank { "MH15KD0001" } else ""
                        )
                    },
                    enabled = !isBusy && displayName.trim().isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("complete_onboarding_button"),
                    shape = MaterialTheme.shapes.large
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Enter KisanDirect",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RoleOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    else MaterialTheme.colorScheme.surface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(if (selected) 2.dp else 1.dp, borderColor, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag(tag),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
fun fetchLastGpsLocation(
    context: android.content.Context,
    onResult: (Double, Double) -> Unit,
    onUnavailable: () -> Unit
) {
    try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    onResult(location.latitude, location.longitude)
                } else {
                    onUnavailable()
                }
            }
            .addOnFailureListener { onUnavailable() }
    } catch (_: Exception) {
        onUnavailable()
    }
}
