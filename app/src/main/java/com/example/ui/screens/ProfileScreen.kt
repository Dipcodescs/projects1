package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PublicProfile
import com.example.data.model.UserPrivateProfile
import com.example.data.model.UserRole
import com.example.data.util.INDIAN_AGRI_HUBS
import com.example.ui.components.RoleBadgeChip
import com.example.ui.components.StarRatingBadge
import com.example.ui.i18n.AppLanguages
import com.example.ui.theme.VerifiedEmerald

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    userProfile: UserPrivateProfile,
    myPublicProfile: PublicProfile?,
    isBusy: Boolean,
    onSaveProfile: (
        displayName: String,
        role: UserRole,
        phone: String,
        farmName: String,
        city: String,
        defaultAddress: String,
        lat: Double,
        lng: Double,
        languageCode: String,
        vehicleType: String,
        vehiclePlate: String,
        driverActive: Boolean
    ) -> Unit,
    onLanguageQuickSwitch: (String) -> Unit
) {
    val context = LocalContext.current

    var displayName by remember(userProfile.userId, userProfile.displayName) { mutableStateOf(userProfile.displayName) }
    var selectedRole by remember(userProfile.userId, userProfile.role) { mutableStateOf(userProfile.roleEnum()) }
    var phone by remember(userProfile.userId, userProfile.phone) { mutableStateOf(userProfile.phone) }
    var farmName by remember(userProfile.userId, userProfile.farmName) { mutableStateOf(userProfile.farmName) }
    var city by remember(userProfile.userId, userProfile.city) { mutableStateOf(userProfile.city) }
    var address by remember(userProfile.userId, userProfile.defaultAddress) { mutableStateOf(userProfile.defaultAddress) }
    var latitude by remember(userProfile.userId, userProfile.latitude) { mutableDoubleStateOf(userProfile.latitude) }
    var longitude by remember(userProfile.userId, userProfile.longitude) { mutableDoubleStateOf(userProfile.longitude) }
    var vehicleType by remember(userProfile.userId, userProfile.vehicleType) {
        mutableStateOf(userProfile.vehicleType.ifBlank { "Mini Truck" })
    }
    var vehiclePlate by remember(userProfile.userId, userProfile.vehiclePlate) { mutableStateOf(userProfile.vehiclePlate) }
    var gpsNotice by remember { mutableStateOf<String?>(null) }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchLastGpsLocation(
                context = context,
                onResult = { lat, lng ->
                    latitude = lat
                    longitude = lng
                    gpsNotice = String.format("GPS updated: %.4f, %.4f", lat, lng)
                },
                onUnavailable = {
                    gpsNotice = "No GPS fix available right now — choose a regional hub below."
                }
            )
        } else {
            gpsNotice = "Location permission denied — select a regional hub below."
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Account Identity Header
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = userProfile.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userProfile.phone.ifBlank { userProfile.city },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RoleBadgeChip(role = userProfile.roleEnum())
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = "KYC verified",
                                    tint = VerifiedEmerald,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = userProfile.kycEnum().label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VerifiedEmerald,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if ((myPublicProfile?.reviewCount ?: 0L) > 0L) {
                        StarRatingBadge(
                            averageRating = myPublicProfile?.averageRating ?: 0.0,
                            reviewCount = myPublicProfile?.reviewCount ?: 0L
                        )
                    }
                }
            }
        }

        // Instant Regional Language Switcher
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Regional Language", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguages.supported.forEach { code ->
                            val langCopy = AppLanguages.forCode(code)
                            FilterChip(
                                selected = userProfile.languageCode == code,
                                onClick = { onLanguageQuickSwitch(code) },
                                label = { Text("${langCopy.nativeLabel} (${langCopy.languageName})") },
                                modifier = Modifier.testTag("profile_lang_$code")
                            )
                        }
                    }
                }
            }
        }

        // Active Role & Workspace Registration Card
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Role & Marketplace Credentials",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Switch seamlessly between Consumer, Farmer, and Delivery Driver workspaces. Farmers manage crop pricing & stock, while Drivers claim and advance deliveries.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UserRole.entries.forEach { role ->
                            FilterChip(
                                selected = selectedRole == role,
                                onClick = { selectedRole = role },
                                label = { Text(role.label) },
                                modifier = Modifier.testTag("profile_role_${role.name.lowercase()}")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { if (it.length <= 60) displayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { if (it.length <= 20) phone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (selectedRole == UserRole.FARMER) {
                        OutlinedTextField(
                            value = farmName,
                            onValueChange = { if (it.length <= 80) farmName = it },
                            label = { Text("Registered Farm / FPO Name") },
                            placeholder = { Text("e.g., Sahyadri Fresh Farms") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_farm_name_input")
                        )
                    }

                    if (selectedRole == UserRole.DRIVER) {
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
                            onValueChange = { if (it.length <= 20) vehiclePlate = it.uppercase() },
                            label = { Text("Driver License Plate") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_vehicle_plate_input")
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GPS & Agricultural Region", style = MaterialTheme.typography.titleSmall)
                        OutlinedButton(
                            onClick = {
                                locationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            },
                            modifier = Modifier.testTag("profile_gps_pin_button")
                        ) {
                            Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Use GPS")
                        }
                    }

                    if (gpsNotice != null) {
                        Text(
                            text = gpsNotice ?: "",
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
                                },
                                label = { Text(hub.city) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { if (it.length <= 200) address = it },
                        label = { Text("Default Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val resolvedFarm = if (selectedRole == UserRole.FARMER && farmName.isBlank()) {
                                "${displayName.trim()}'s Farm"
                            } else {
                                farmName.trim()
                            }
                            onSaveProfile(
                                displayName.trim().ifBlank { userProfile.displayName },
                                selectedRole,
                                phone.trim(),
                                resolvedFarm,
                                city,
                                address.trim().ifBlank { city },
                                latitude,
                                longitude,
                                userProfile.languageCode,
                                if (selectedRole == UserRole.DRIVER) vehicleType else userProfile.vehicleType,
                                if (selectedRole == UserRole.DRIVER) vehiclePlate.trim().ifBlank { "MH15KD0001" } else userProfile.vehiclePlate,
                                if (selectedRole == UserRole.DRIVER) true else userProfile.driverActive
                            )
                        },
                        enabled = !isBusy && displayName.trim().isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_profile_button"),
                        shape = MaterialTheme.shapes.large
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save Role & Account Settings", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
