package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.catalog.HarvestCatalog
import com.example.data.model.CartItem
import com.example.data.model.FavoriteProduct
import com.example.data.model.Order
import com.example.data.model.OrganicProduct
import com.example.data.model.UserProfile
import com.example.data.repository.PahadRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

class PahadViewModel(
  private val repository: PahadRepository,
  private val currentUserId: String,
  private val initialDisplayName: String,
  private val initialEmail: String?,
) : ViewModel() {

  private val _selectedCategory = MutableStateFlow("All")
  val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedProduct = MutableStateFlow<OrganicProduct?>(null)
  val selectedProduct: StateFlow<OrganicProduct?> = _selectedProduct.asStateFlow()

  private val _bannerMessage = MutableStateFlow<String?>(null)
  val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  val filteredProducts: StateFlow<List<OrganicProduct>> =
    combine(_selectedCategory, _searchQuery) { category, query ->
      val trimmed = query.trim()
      HarvestCatalog.products.filter { product ->
        val matchesCategory = category == "All" || product.category == category
        val matchesQuery =
          trimmed.isEmpty() ||
            product.name.contains(trimmed, ignoreCase = true) ||
            product.region.contains(trimmed, ignoreCase = true) ||
            product.description.contains(trimmed, ignoreCase = true)
        matchesCategory && matchesQuery
      }
    }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = HarvestCatalog.products,
      )

  val profileState: StateFlow<UiState<UserProfile?>> =
    repository
      .observeUserProfile(currentUserId)
      .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
      .catch { error ->
        val rootError = unwrapFirestoreOrThrowCancellation(error)
        Log.w(TAG, "Error observing profile", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load profile"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val cartState: StateFlow<UiState<List<CartItem>>> =
    repository
      .observeCartItems(currentUserId)
      .map<List<CartItem>, UiState<List<CartItem>>> { UiState.Success(it) }
      .catch { error ->
        val rootError = unwrapFirestoreOrThrowCancellation(error)
        Log.w(TAG, "Error observing cart", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load basket"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val ordersState: StateFlow<UiState<List<Order>>> =
    repository
      .observeOrders(currentUserId)
      .map<List<Order>, UiState<List<Order>>> { UiState.Success(it) }
      .catch { error ->
        val rootError = unwrapFirestoreOrThrowCancellation(error)
        Log.w(TAG, "Error observing orders", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load orders"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val favoritesState: StateFlow<UiState<List<FavoriteProduct>>> =
    repository
      .observeFavorites(currentUserId)
      .map<List<FavoriteProduct>, UiState<List<FavoriteProduct>>> { UiState.Success(it) }
      .catch { error ->
        val rootError = unwrapFirestoreOrThrowCancellation(error)
        Log.w(TAG, "Error observing favorites", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load favorites"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  init {
    viewModelScope.launch {
      repository.ensureUserProfile(initialDisplayName, initialEmail)
    }
  }

  private fun unwrapFirestoreOrThrowCancellation(error: Throwable): Throwable {
    val firestoreError =
      generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    return firestoreError ?: error
  }

  fun selectCategory(category: String) {
    _selectedCategory.value = category
  }

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun openProductDetail(product: OrganicProduct) {
    _selectedProduct.value = product
  }

  fun closeProductDetail() {
    _selectedProduct.value = null
  }

  fun clearBannerMessage() {
    _bannerMessage.value = null
  }

  fun addToCart(product: OrganicProduct) {
    viewModelScope.launch {
      val result = repository.addToCart(product)
      _bannerMessage.value =
        if (result.isSuccess) {
          "Added ${product.name} to your basket"
        } else {
          result.exceptionOrNull()?.localizedMessage ?: "Could not add item to basket"
        }
    }
  }

  fun updateCartQuantity(cartItemId: String, newQuantity: Int) {
    viewModelScope.launch {
      val result = repository.updateCartQuantity(cartItemId, newQuantity)
      if (result.isFailure) {
        _bannerMessage.value =
          result.exceptionOrNull()?.localizedMessage ?: "Failed to update basket quantity"
      }
    }
  }

  fun removeCartItem(cartItemId: String) {
    viewModelScope.launch {
      val result = repository.removeCartItem(cartItemId)
      if (result.isFailure) {
        _bannerMessage.value =
          result.exceptionOrNull()?.localizedMessage ?: "Failed to remove item from basket"
      }
    }
  }

  fun placeOrder(cartItems: List<CartItem>, deliveryAddress: String, onSuccess: () -> Unit) {
    viewModelScope.launch {
      _isBusy.value = true
      val result = repository.placeOrder(cartItems, deliveryAddress)
      _isBusy.value = false
      if (result.isSuccess) {
        _bannerMessage.value = "Harvest order placed! Thank you for supporting Pahad growers."
        onSuccess()
      } else {
        _bannerMessage.value =
          result.exceptionOrNull()?.localizedMessage ?: "Unable to place order."
      }
    }
  }

  fun cancelOrder(orderId: String) {
    viewModelScope.launch {
      val result = repository.cancelOrder(orderId)
      _bannerMessage.value =
        if (result.isSuccess) {
          "Order cancelled."
        } else {
          result.exceptionOrNull()?.localizedMessage ?: "Could not cancel order."
        }
    }
  }

  fun toggleFavorite(product: OrganicProduct, currentlyFavorited: Boolean) {
    viewModelScope.launch {
      val result = repository.toggleFavorite(product, currentlyFavorited)
      if (result.isFailure) {
        _bannerMessage.value =
          result.exceptionOrNull()?.localizedMessage ?: "Could not update favorite."
      }
    }
  }

  fun saveProfile(displayName: String, deliveryAddress: String, preferredRegion: String) {
    viewModelScope.launch {
      _isBusy.value = true
      val result = repository.updateUserProfile(displayName, deliveryAddress, preferredRegion)
      _isBusy.value = false
      _bannerMessage.value =
        if (result.isSuccess) {
          "Pantry preferences synced to Cloud Firestore"
        } else {
          result.exceptionOrNull()?.localizedMessage ?: "Failed to save profile."
        }
    }
  }

  private companion object {
    const val TAG = "PahadViewModel"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
