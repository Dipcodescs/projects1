package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CartEntry
import com.example.data.model.DeliveryStatus
import com.example.data.model.FarmerReview
import com.example.data.model.KycStatus
import com.example.data.model.ListingWithMeta
import com.example.data.model.MarketplaceOrder
import com.example.data.model.PayoutEntryType
import com.example.data.model.PayoutLedgerEntry
import com.example.data.model.ProduceCategory
import com.example.data.model.ProduceListing
import com.example.data.model.PublicProfile
import com.example.data.model.SplitSettlementSummary
import com.example.data.model.UserPrivateProfile
import com.example.data.model.UserRole
import com.example.data.repository.MarketplaceRepository
import com.example.data.util.computeDriverDeliveryFee
import com.example.data.util.haversineDistanceKm
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class DriverEarningsSummary(
    val totalEarned: Double = 0.0,
    val totalWithdrawn: Double = 0.0,
    val availableBalance: Double = 0.0,
    val pendingInTransit: Double = 0.0,
    val completedCount: Int = 0
)

class KisanDirectViewModel(
    private val repository: MarketplaceRepository,
    val currentUserId: String = MarketplaceRepository.LOCAL_USER_ID
) : ViewModel() {

    // --- Reactive Local Database Streams ---

    val userProfileState: StateFlow<UiState<UserPrivateProfile?>> =
        repository.observeUserProfile(currentUserId)
            .map<UserPrivateProfile?, UiState<UserPrivateProfile?>> { UiState.Success(it) }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing user profile", error)
                emit(UiState.Error(error.message ?: "Failed to load account profile"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val publicProfiles: StateFlow<Map<String, PublicProfile>> =
        repository.observePublicProfiles()
            .map { list -> list.associateBy { it.userId.ifBlank { it.id } } }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing public profiles", error)
                emit(emptyMap())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyMap())

    val activeListingsState: StateFlow<UiState<List<ProduceListing>>> =
        repository.observeActiveListings()
            .map<List<ProduceListing>, UiState<List<ProduceListing>>> { list ->
                UiState.Success(list.sortedByDescending { it.createdAtMillis })
            }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing active listings", error)
                emit(UiState.Error(error.message ?: "Failed to load nearby produce"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val farmerOwnListings: StateFlow<List<ProduceListing>> =
        repository.observeFarmerListings(currentUserId)
            .map { list -> list.sortedByDescending { it.createdAtMillis } }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing farmer listings", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val consumerOrdersState: StateFlow<UiState<List<MarketplaceOrder>>> =
        repository.observeConsumerOrders(currentUserId)
            .map<List<MarketplaceOrder>, UiState<List<MarketplaceOrder>>> { list ->
                UiState.Success(list.sortedByDescending { it.createdAtMillis })
            }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing consumer orders", error)
                emit(UiState.Error(error.message ?: "Failed to load orders"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val farmerIncomingOrders: StateFlow<List<MarketplaceOrder>> =
        repository.observeFarmerOrders(currentUserId)
            .map { list -> list.sortedByDescending { it.createdAtMillis } }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing farmer incoming orders", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val availableDeliveryTasks: StateFlow<List<MarketplaceOrder>> =
        repository.observeAvailableDeliveryTasks()
            .map { list -> list.sortedByDescending { it.createdAtMillis } }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing available delivery tasks", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val driverAssignedOrders: StateFlow<List<MarketplaceOrder>> =
        repository.observeDriverAssignedOrders(currentUserId)
            .map { list -> list.sortedByDescending { it.createdAtMillis } }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing driver orders", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val allVerifiedReviews: StateFlow<List<FarmerReview>> =
        repository.observeVerifiedReviews()
            .map { list -> list.sortedByDescending { it.createdAtMillis } }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing verified reviews", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val payoutLedger: StateFlow<List<PayoutLedgerEntry>> =
        repository.observePayoutLedger(currentUserId)
            .map { list -> list.sortedByDescending { it.createdAtMillis } }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing payout ledger", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    // --- Interactive Filter, Cart & Feedback State ---

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<ProduceCategory?>(null)
    val selectedCategory: StateFlow<ProduceCategory?> = _selectedCategory.asStateFlow()

    // Default 25 km radius matching spec (`radius_km=25`); null means All distances
    private val _radiusKm = MutableStateFlow<Double?>(25.0)
    val radiusKm: StateFlow<Double?> = _radiusKm.asStateFlow()

    private val _cart = MutableStateFlow<Map<String, CartEntry>>(emptyMap())
    val cart: StateFlow<Map<String, CartEntry>> = _cart.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    val filteredListings: StateFlow<List<ListingWithMeta>> = combine(
        activeListingsState,
        publicProfiles,
        userProfileState,
        searchQuery,
        selectedCategory,
        radiusKm
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val listingsState = values[0] as UiState<List<ProduceListing>>
        @Suppress("UNCHECKED_CAST")
        val profiles = values[1] as Map<String, PublicProfile>
        @Suppress("UNCHECKED_CAST")
        val userState = values[2] as UiState<UserPrivateProfile?>
        val query = (values[3] as String).trim()
        val category = values[4] as ProduceCategory?
        val maxRadius = values[5] as Double?

        val rawList = (listingsState as? UiState.Success)?.data.orEmpty()
        val user = (userState as? UiState.Success)?.data
        val userLat = user?.latitude ?: 19.9975
        val userLng = user?.longitude ?: 73.7898

        rawList.map { listing ->
            val dist = haversineDistanceKm(userLat, userLng, listing.latitude, listing.longitude)
            ListingWithMeta(
                listing = listing,
                distanceKm = dist,
                farmerProfile = profiles[listing.farmerId]
            )
        }.filter { item ->
            val matchesSearch = query.isEmpty() ||
                item.listing.cropName.contains(query, ignoreCase = true) ||
                item.listing.farmName.contains(query, ignoreCase = true) ||
                item.listing.farmerName.contains(query, ignoreCase = true) ||
                item.listing.city.contains(query, ignoreCase = true)
            val matchesCat = category == null || item.listing.categoryEnum() == category
            val matchesRadius = maxRadius == null || item.distanceKm <= maxRadius
            matchesSearch && matchesCat && matchesRadius && item.listing.availableQtyKg > 0.0
        }.sortedBy { it.distanceKm }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val driverEarningsSummary: StateFlow<DriverEarningsSummary> = combine(
        payoutLedger,
        driverAssignedOrders
    ) { ledger, assigned ->
        val deliveryEarnings = ledger
            .filter { it.typeEnum() == PayoutEntryType.DELIVERY_EARNING }
            .sumOf { it.amount }
        val withdrawn = ledger
            .filter { it.typeEnum() == PayoutEntryType.WITHDRAWAL }
            .sumOf { it.amount }
        val inTransit = assigned
            .filter { it.deliveryStatusEnum() != DeliveryStatus.DELIVERED }
            .sumOf { it.driverPayoutAmount }
        val completed = assigned.count { it.deliveryStatusEnum() == DeliveryStatus.DELIVERED }
        DriverEarningsSummary(
            totalEarned = round2(deliveryEarnings),
            totalWithdrawn = round2(withdrawn),
            availableBalance = round2((deliveryEarnings - withdrawn).coerceAtLeast(0.0)),
            pendingInTransit = round2(inTransit),
            completedCount = completed
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        DriverEarningsSummary()
    )

    // --- UI Filter & Cart Controls ---

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: ProduceCategory?) {
        _selectedCategory.value = category
    }

    fun selectRadiusKm(radius: Double?) {
        _radiusKm.value = radius
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun showMessage(message: String) {
        _statusBanner.value = message
    }

    fun addToCart(listing: ProduceListing, stepKg: Double = 1.0) {
        _cart.update { current ->
            val existing = current[listing.id]
            val nextQty = ((existing?.quantityKg ?: 0.0) + stepKg)
                .coerceAtMost(listing.availableQtyKg)
            if (nextQty <= 0.0) current
            else current + (listing.id to CartEntry(listing = listing, quantityKg = round2(nextQty)))
        }
    }

    fun decrementCartItem(listingId: String, stepKg: Double = 1.0) {
        _cart.update { current ->
            val existing = current[listingId] ?: return@update current
            val nextQty = round2(existing.quantityKg - stepKg)
            if (nextQty <= 0.0) current - listingId
            else current + (listingId to existing.copy(quantityKg = nextQty))
        }
    }

    fun clearCart() {
        _cart.value = emptyMap()
    }

    fun computeSplitSummary(userLat: Double, userLng: Double): SplitSettlementSummary {
        val entries = _cart.value.values.toList()
        if (entries.isEmpty()) {
            return SplitSettlementSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }
        val subtotal = round2(entries.sumOf { it.lineTotal })
        val farmerDirect = round2(subtotal * 0.96)
        val platformFee = round2((subtotal - farmerDirect).coerceAtLeast(0.0))
        val driverTotal = round2(
            entries.groupBy { it.listing.farmerId }.values.sumOf { group ->
                val first = group.first().listing
                val dist = haversineDistanceKm(first.latitude, first.longitude, userLat, userLng)
                computeDriverDeliveryFee(dist, group.sumOf { it.quantityKg })
            }
        )
        val retailCompareTotal = round2(entries.sumOf {
            it.listing.marketComparePricePerKg.coerceAtLeast(it.listing.pricePerKg) * it.quantityKg
        })
        val savings = round2((retailCompareTotal - subtotal).coerceAtLeast(0.0))
        return SplitSettlementSummary(
            subtotal = subtotal,
            farmerDirectPayout = farmerDirect,
            driverDeliveryPayout = driverTotal,
            platformFee = platformFee,
            grandTotal = round2(subtotal + driverTotal),
            estimatedMiddlemanSavings = savings
        )
    }

    // --- Mutations ---

    fun saveProfile(
        displayName: String,
        role: UserRole,
        phone: String,
        farmName: String,
        city: String,
        defaultAddress: String,
        latitude: Double,
        longitude: Double,
        languageCode: String,
        vehicleType: String,
        vehiclePlate: String,
        driverActive: Boolean,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveUserProfile(
                userId = currentUserId,
                displayName = displayName,
                role = role,
                phone = phone,
                farmName = farmName,
                city = city,
                defaultAddress = defaultAddress,
                latitude = latitude,
                longitude = longitude,
                languageCode = languageCode,
                vehicleType = vehicleType,
                vehiclePlate = vehiclePlate,
                driverActive = driverActive,
                kycStatus = KycStatus.VERIFIED
            )
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    _statusBanner.value = "Account updated (${role.label})"
                    onComplete()
                },
                onFailure = { err ->
                    _statusBanner.value = err.message ?: "Failed to save profile"
                }
            )
        }
    }

    fun setDriverAvailability(active: Boolean) {
        viewModelScope.launch {
            val result = repository.updateDriverAvailability(active, currentUserId)
            result.onFailure { _statusBanner.value = it.message ?: "Unable to update availability" }
        }
    }

    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            val result = repository.updateLanguage(languageCode, currentUserId)
            result.onFailure { _statusBanner.value = it.message ?: "Unable to switch language" }
        }
    }

    fun publishProduce(
        profile: UserPrivateProfile,
        cropName: String,
        category: ProduceCategory,
        pricePerKg: Double,
        marketComparePricePerKg: Double,
        availableQtyKg: Double,
        harvestDate: String,
        description: String,
        city: String,
        latitude: Double,
        longitude: Double,
        photoBase64: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.publishListing(
                profile = profile,
                cropName = cropName,
                category = category,
                pricePerKg = pricePerKg,
                marketComparePricePerKg = marketComparePricePerKg,
                availableQtyKg = availableQtyKg,
                harvestDate = harvestDate,
                description = description,
                city = city,
                latitude = latitude,
                longitude = longitude,
                photoBase64 = photoBase64
            )
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    _statusBanner.value = "Harvest published live to nearby consumers!"
                    onSuccess()
                },
                onFailure = { err ->
                    _statusBanner.value = err.message ?: "Failed to publish listing"
                }
            )
        }
    }

    fun updateListingStockAndPrice(listingId: String, pricePerKg: Double, availableQtyKg: Double) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateListingInventory(listingId, pricePerKg, availableQtyKg)
            _isBusy.value = false
            result.fold(
                onSuccess = { _statusBanner.value = "Inventory updated" },
                onFailure = { _statusBanner.value = it.message ?: "Failed to update inventory" }
            )
        }
    }

    fun deleteListing(listingId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteListing(listingId)
            _isBusy.value = false
            result.fold(
                onSuccess = { _statusBanner.value = "Listing removed" },
                onFailure = { _statusBanner.value = it.message ?: "Failed to remove listing" }
            )
        }
    }

    fun checkoutCart(
        consumerProfile: UserPrivateProfile,
        deliveryAddress: String,
        deliveryLat: Double,
        deliveryLng: Double,
        onSuccess: () -> Unit
    ) {
        val entries = _cart.value.values.toList()
        if (entries.isEmpty()) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.placeOrdersAtomically(
                consumerProfile = consumerProfile,
                cartEntries = entries,
                deliveryAddress = deliveryAddress,
                deliveryLat = deliveryLat,
                deliveryLng = deliveryLng
            )
            _isBusy.value = false
            result.fold(
                onSuccess = { ids ->
                    _cart.value = emptyMap()
                    _statusBanner.value =
                        "Split checkout complete (${ids.size} farm order${if (ids.size > 1) "s" else ""} locked & confirmed)!"
                    onSuccess()
                },
                onFailure = { err ->
                    _statusBanner.value = err.message ?: "Checkout failed — inventory may have changed"
                }
            )
        }
    }

    fun acceptDeliveryTask(orderId: String, driverProfile: UserPrivateProfile) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.acceptDeliveryTask(orderId, driverProfile)
            _isBusy.value = false
            result.fold(
                onSuccess = { _statusBanner.value = "Delivery task assigned to you!" },
                onFailure = { _statusBanner.value = it.message ?: "Could not accept task" }
            )
        }
    }

    fun advanceDelivery(order: MarketplaceOrder, nextStatus: DeliveryStatus) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.advanceDeliveryStatus(order, nextStatus, currentUserId)
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    _statusBanner.value = if (nextStatus == DeliveryStatus.DELIVERED) {
                        "Delivery confirmed! Payout credited to your earnings ledger."
                    } else {
                        "Status updated to ${nextStatus.label}"
                    }
                },
                onFailure = { _statusBanner.value = it.message ?: "Status update failed" }
            )
        }
    }

    fun withdrawDriverEarnings(amount: Double, upiOrBankNote: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.recordWithdrawal(amount, upiOrBankNote, currentUserId)
            _isBusy.value = false
            result.fold(
                onSuccess = { _statusBanner.value = "Withdrawal recorded in your payout ledger!" },
                onFailure = { _statusBanner.value = it.message ?: "Withdrawal failed" }
            )
        }
    }

    fun submitFarmerReview(
        order: MarketplaceOrder,
        rating: Int,
        comment: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.submitVerifiedFarmerReview(order, rating, comment, currentUserId)
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    _statusBanner.value = "Verified review published & farmer rating updated!"
                    onSuccess()
                },
                onFailure = { _statusBanner.value = it.message ?: "Unable to submit review" }
            )
        }
    }

    private fun round2(value: Double): Double = (value * 100.0).roundToInt() / 100.0

    private companion object {
        const val TAG = "KisanDirectVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
