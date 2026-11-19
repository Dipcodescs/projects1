package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.KisanDirectDatabase
import com.example.data.model.UserPrivateProfile
import com.example.data.model.UserRole
import com.example.data.repository.MarketplaceRepository
import com.example.ui.i18n.AppLanguages
import com.example.ui.screens.DriverHubScreen
import com.example.ui.screens.FarmerHubScreen
import com.example.ui.screens.MarketScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.KisanDirectTheme
import com.example.ui.viewmodel.KisanDirectViewModel
import com.example.ui.viewmodel.UiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KisanDirectTheme {
                KisanAppRoot()
            }
        }
    }
}

private data class NavDestination(
    val index: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
fun KisanAppRoot() {
    val viewModel: KisanDirectViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val database = KisanDirectDatabase.getInstance(app)
                KisanDirectViewModel(
                    repository = MarketplaceRepository(database),
                    currentUserId = MarketplaceRepository.LOCAL_USER_ID
                )
            }
        }
    )

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusBanner) {
        statusBanner?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearBanner()
        }
    }

    when (val state = userProfileState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Unable to open KisanDirect local store: ${state.message}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        is UiState.Success -> {
            val profile = state.data
            if (profile == null) {
                OnboardingScreen(
                    isBusy = isBusy,
                    onCompleteOnboarding = { name, role, phone, farm, city, addr, lat, lng, lang, vehicle, plate ->
                        viewModel.saveProfile(
                            displayName = name,
                            role = role,
                            phone = phone,
                            farmName = farm,
                            city = city,
                            defaultAddress = addr,
                            latitude = lat,
                            longitude = lng,
                            languageCode = lang,
                            vehicleType = vehicle,
                            vehiclePlate = plate,
                            driverActive = (role == UserRole.DRIVER)
                        )
                    }
                )
            } else {
                KisanMainShell(
                    profile = profile,
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

@Composable
private fun KisanMainShell(
    profile: UserPrivateProfile,
    viewModel: KisanDirectViewModel,
    snackbarHostState: SnackbarHostState
) {
    val activeRole = profile.roleEnum()
    val copy = remember(profile.languageCode) { AppLanguages.forCode(profile.languageCode) }

    var selectedTab by remember { mutableIntStateOf(0) }

    // Return to Market tab on system back press from secondary tabs
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    val publicProfiles by viewModel.publicProfiles.collectAsStateWithLifecycle()
    val activeListingsState by viewModel.activeListingsState.collectAsStateWithLifecycle()
    val filteredListings by viewModel.filteredListings.collectAsStateWithLifecycle()
    val farmerOwnListings by viewModel.farmerOwnListings.collectAsStateWithLifecycle()
    val consumerOrdersState by viewModel.consumerOrdersState.collectAsStateWithLifecycle()
    val farmerIncomingOrders by viewModel.farmerIncomingOrders.collectAsStateWithLifecycle()
    val availableDeliveryTasks by viewModel.availableDeliveryTasks.collectAsStateWithLifecycle()
    val driverAssignedOrders by viewModel.driverAssignedOrders.collectAsStateWithLifecycle()
    val allVerifiedReviews by viewModel.allVerifiedReviews.collectAsStateWithLifecycle()
    val payoutLedger by viewModel.payoutLedger.collectAsStateWithLifecycle()
    val driverEarnings by viewModel.driverEarningsSummary.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val radiusKm by viewModel.radiusKm.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val workspaceDest = when (activeRole) {
        UserRole.FARMER -> NavDestination(
            index = 2,
            label = copy.navFarmerHub,
            selectedIcon = Icons.Filled.Agriculture,
            unselectedIcon = Icons.Outlined.Agriculture,
            tag = "nav_farmer_hub"
        )
        UserRole.DRIVER -> NavDestination(
            index = 2,
            label = copy.navDriverHub,
            selectedIcon = Icons.Filled.LocalShipping,
            unselectedIcon = Icons.Outlined.LocalShipping,
            tag = "nav_driver_hub"
        )
        UserRole.CONSUMER -> NavDestination(
            index = 2,
            label = copy.navFarmMap,
            selectedIcon = Icons.Filled.Map,
            unselectedIcon = Icons.Outlined.Map,
            tag = "nav_farm_map"
        )
    }

    val destinations = listOf(
        NavDestination(0, copy.navMarket, Icons.Filled.Storefront, Icons.Outlined.Storefront, "nav_market"),
        NavDestination(1, copy.navOrders, Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "nav_orders"),
        workspaceDest,
        NavDestination(3, copy.navAccount, Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle, "nav_account")
    )

    val splitSummary = remember(cart, profile.latitude, profile.longitude) {
        viewModel.computeSplitSummary(profile.latitude, profile.longitude)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar {
                        destinations.forEach { dest ->
                            val isSelected = selectedTab == dest.index
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTab = dest.index },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                modifier = Modifier.testTag(dest.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail {
                        destinations.forEach { dest ->
                            val isSelected = selectedTab == dest.index
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { selectedTab = dest.index },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                modifier = Modifier.testTag(dest.tag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp)
                    ) {
                        when (selectedTab) {
                            0 -> MarketScreen(
                                copy = copy,
                                userProfile = profile,
                                listingsState = activeListingsState,
                                filteredListings = filteredListings,
                                allReviews = allVerifiedReviews,
                                publicProfiles = publicProfiles,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                radiusKm = radiusKm,
                                cart = cart,
                                splitSummary = splitSummary,
                                isBusy = isBusy,
                                mapOnlyMode = false,
                                onSearchChanged = viewModel::updateSearchQuery,
                                onCategorySelected = viewModel::selectCategory,
                                onRadiusSelected = viewModel::selectRadiusKm,
                                onAddToCart = { viewModel.addToCart(it) },
                                onDecrementCart = { viewModel.decrementCartItem(it) },
                                onCheckout = { address, onDone ->
                                    viewModel.checkoutCart(
                                        consumerProfile = profile,
                                        deliveryAddress = address,
                                        deliveryLat = profile.latitude,
                                        deliveryLng = profile.longitude,
                                        onSuccess = {
                                            onDone()
                                            selectedTab = 1
                                        }
                                    )
                                },
                                onNavigateToAccount = { selectedTab = 3 }
                            )

                            1 -> OrdersScreen(
                                copy = copy,
                                activeRole = activeRole,
                                currentUserId = viewModel.currentUserId,
                                consumerOrdersState = consumerOrdersState,
                                farmerIncomingOrders = farmerIncomingOrders,
                                isBusy = isBusy,
                                onSubmitReview = { order, stars, comment, onDone ->
                                    viewModel.submitFarmerReview(order, stars, comment, onDone)
                                },
                                onBrowseMarket = { selectedTab = 0 }
                            )

                            2 -> when (activeRole) {
                                UserRole.FARMER -> FarmerHubScreen(
                                    copy = copy,
                                    userProfile = profile,
                                    myPublicProfile = publicProfiles[viewModel.currentUserId],
                                    ownListings = farmerOwnListings,
                                    incomingOrders = farmerIncomingOrders,
                                    myReviews = allVerifiedReviews.filter { it.farmerId == viewModel.currentUserId },
                                    isBusy = isBusy,
                                    onPublishListing = { name, cat, price, compare, qty, date, desc, photo, onDone ->
                                        viewModel.publishProduce(
                                            profile = profile,
                                            cropName = name,
                                            category = cat,
                                            pricePerKg = price,
                                            marketComparePricePerKg = compare,
                                            availableQtyKg = qty,
                                            harvestDate = date,
                                            description = desc,
                                            city = profile.city,
                                            latitude = profile.latitude,
                                            longitude = profile.longitude,
                                            photoBase64 = photo,
                                            onSuccess = onDone
                                        )
                                    },
                                    onUpdateListing = viewModel::updateListingStockAndPrice,
                                    onDeleteListing = viewModel::deleteListing,
                                    onSwitchToFarmerRole = { selectedTab = 3 }
                                )

                                UserRole.DRIVER -> DriverHubScreen(
                                    copy = copy,
                                    userProfile = profile,
                                    availableTasks = availableDeliveryTasks,
                                    assignedOrders = driverAssignedOrders,
                                    earnings = driverEarnings,
                                    payoutLedger = payoutLedger,
                                    isBusy = isBusy,
                                    onRegisterAsDriver = { vehicleType, plate ->
                                        viewModel.saveProfile(
                                            displayName = profile.displayName,
                                            role = UserRole.DRIVER,
                                            phone = profile.phone,
                                            farmName = profile.farmName,
                                            city = profile.city,
                                            defaultAddress = profile.defaultAddress,
                                            latitude = profile.latitude,
                                            longitude = profile.longitude,
                                            languageCode = profile.languageCode,
                                            vehicleType = vehicleType,
                                            vehiclePlate = plate,
                                            driverActive = true
                                        )
                                    },
                                    onToggleOnline = viewModel::setDriverAvailability,
                                    onAcceptTask = { orderId -> viewModel.acceptDeliveryTask(orderId, profile) },
                                    onAdvanceStatus = viewModel::advanceDelivery,
                                    onWithdraw = viewModel::withdrawDriverEarnings
                                )

                                UserRole.CONSUMER -> MarketScreen(
                                    copy = copy,
                                    userProfile = profile,
                                    listingsState = activeListingsState,
                                    filteredListings = filteredListings,
                                    allReviews = allVerifiedReviews,
                                    publicProfiles = publicProfiles,
                                    searchQuery = searchQuery,
                                    selectedCategory = selectedCategory,
                                    radiusKm = radiusKm,
                                    cart = cart,
                                    splitSummary = splitSummary,
                                    isBusy = isBusy,
                                    mapOnlyMode = true,
                                    onSearchChanged = viewModel::updateSearchQuery,
                                    onCategorySelected = viewModel::selectCategory,
                                    onRadiusSelected = viewModel::selectRadiusKm,
                                    onAddToCart = { viewModel.addToCart(it) },
                                    onDecrementCart = { viewModel.decrementCartItem(it) },
                                    onCheckout = { address, onDone ->
                                        viewModel.checkoutCart(
                                            consumerProfile = profile,
                                            deliveryAddress = address,
                                            deliveryLat = profile.latitude,
                                            deliveryLng = profile.longitude,
                                            onSuccess = {
                                                onDone()
                                                selectedTab = 1
                                            }
                                        )
                                    },
                                    onNavigateToAccount = { selectedTab = 3 }
                                )
                            }

                            else -> ProfileScreen(
                                userProfile = profile,
                                myPublicProfile = publicProfiles[viewModel.currentUserId],
                                isBusy = isBusy,
                                onSaveProfile = { name, role, phone, farm, city, addr, lat, lng, lang, vehicle, plate, active ->
                                    viewModel.saveProfile(
                                        displayName = name,
                                        role = role,
                                        phone = phone,
                                        farmName = farm,
                                        city = city,
                                        defaultAddress = addr,
                                        latitude = lat,
                                        longitude = lng,
                                        languageCode = lang,
                                        vehicleType = vehicle,
                                        vehiclePlate = plate,
                                        driverActive = active
                                    )
                                },
                                onLanguageQuickSwitch = viewModel::setLanguage
                            )
                        }
                    }
                }
            }
        }
    }
}
