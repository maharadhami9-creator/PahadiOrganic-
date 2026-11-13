package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CartItem
import com.example.data.model.FavoriteProduct
import com.example.data.model.Order
import com.example.data.model.OrganicProduct
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PahadRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(
    context: Context
  ) : this(
    db =
      FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
      ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
  }

  private fun handleFlowError(error: Throwable, operation: OperationType, path: String, uid: String) {
    val firestoreError =
      generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
      handleFirestoreError(exceptionToLog, operation, path, auth)
    }
  }

  suspend fun ensureUserProfile(displayName: String, email: String?): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        val safeName = displayName.trim().ifBlank { "Pahad Member" }.take(80)
        val payload =
          buildMap<String, Any> {
            put("userId", uid)
            put("displayName", safeName)
            email?.trim()?.takeIf { it.isNotEmpty() }?.let { put("email", it.take(120)) }
            put("deliveryAddress", "")
            put("preferredRegion", "Kumaon")
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
          }
        docRef.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
      Result.failure(e)
    }
  }

  fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    val path = "users/$userId"
    emitAll(
      db.collection("users")
        .document(userId)
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
          } else {
            null
          }
        }
        .catch { error ->
          handleFlowError(error, OperationType.GET, path, userId)
          throw error
        }
    )
  }

  suspend fun updateUserProfile(
    displayName: String,
    deliveryAddress: String,
    preferredRegion: String,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val safeName = displayName.trim().ifBlank { "Pahad Member" }.take(80)
    val safeAddress = deliveryAddress.trim().take(250)
    val safeRegion = preferredRegion.trim().ifBlank { "Kumaon" }.take(60)
    return try {
      val snapshot = docRef.get().await()
      if (snapshot.exists()) {
        docRef
          .update(
            mapOf(
              "displayName" to safeName,
              "deliveryAddress" to safeAddress,
              "preferredRegion" to safeRegion,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        val email = auth.currentUser?.email?.trim()?.takeIf { it.isNotEmpty() }?.take(120)
        val payload =
          buildMap<String, Any> {
            put("userId", uid)
            put("displayName", safeName)
            if (email != null) put("email", email)
            put("deliveryAddress", safeAddress)
            put("preferredRegion", safeRegion)
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
          }
        docRef.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  fun observeCartItems(userId: String): Flow<List<CartItem>> = flow {
    val path = "users/$userId/cartItems"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("cartItems")
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(CartItem::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          handleFlowError(error, OperationType.LIST, path, userId)
          throw error
        }
    )
  }

  suspend fun addToCart(product: OrganicProduct): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("cartItems").document(product.id)
    return try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        val currentQty = (existing.getLong("quantity") ?: 1L).toInt()
        val nextQty = (currentQty + 1).coerceAtMost(99)
        docRef
          .update(
            mapOf(
              "quantity" to nextQty,
              "unitPrice" to product.priceInr,
              "unitLabel" to product.unitLabel.take(30),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        val payload =
          mapOf(
            "userId" to uid,
            "productId" to product.id,
            "productName" to product.name.take(100),
            "category" to product.category.take(40),
            "unitLabel" to product.unitLabel.take(30),
            "unitPrice" to product.priceInr,
            "quantity" to 1,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun updateCartQuantity(cartItemId: String, newQuantity: Int): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("cartItems").document(cartItemId)
    return try {
      if (newQuantity <= 0) {
        docRef.delete().await()
      } else {
        docRef
          .update(
            mapOf(
              "quantity" to newQuantity.coerceIn(1, 99),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun removeCartItem(cartItemId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("cartItems").document(cartItemId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      Result.failure(e)
    }
  }

  fun observeOrders(userId: String): Flow<List<Order>> = flow {
    val path = "users/$userId/orders"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("orders")
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(Order::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          handleFlowError(error, OperationType.LIST, path, userId)
          throw error
        }
    )
  }

  suspend fun placeOrder(cartItems: List<CartItem>, deliveryAddress: String): Result<String> {
    val uid = requireUserId()
    if (cartItems.isEmpty()) {
      return Result.failure(IllegalArgumentException("Your basket is empty."))
    }
    val safeAddress = deliveryAddress.trim().take(250)
    if (safeAddress.isBlank()) {
      return Result.failure(IllegalArgumentException("Please enter a delivery address."))
    }

    val orderDocRef = db.collection("users").document(uid).collection("orders").document()
    val itemCount = cartItems.sumOf { it.quantity }.toInt().coerceIn(1, 500)
    val totalAmount = cartItems.sumOf { it.unitPrice * it.quantity }
    val itemSummary =
      cartItems
        .joinToString(", ") { "${it.productName} × ${it.quantity}" }
        .take(500)
        .ifBlank { "Organic Harvest Order" }
    val orderNumber = "PO-${(System.currentTimeMillis() % 900000L + 100000L)}"

    val payload =
      mapOf(
        "userId" to uid,
        "orderNumber" to orderNumber,
        "itemSummary" to itemSummary,
        "itemCount" to itemCount,
        "totalAmount" to totalAmount,
        "deliveryAddress" to safeAddress,
        "status" to "placed",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )

    return try {
      val batch = db.batch()
      batch.set(orderDocRef, payload)
      cartItems.forEach { item ->
        val cartDocId = item.id.ifBlank { item.productId }
        if (cartDocId.isNotBlank()) {
          val cartRef =
            db.collection("users").document(uid).collection("cartItems").document(cartDocId)
          batch.delete(cartRef)
        }
      }
      batch.commit().await()
      Result.success(orderDocRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, orderDocRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun cancelOrder(orderId: String): Result<Unit> {
    val uid = requireUserId()
    require(orderId.isNotBlank()) { "Order ID must not be blank" }
    val docRef = db.collection("users").document(uid).collection("orders").document(orderId)
    return try {
      docRef
        .update(
          mapOf(
            "status" to "cancelled",
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  fun observeFavorites(userId: String): Flow<List<FavoriteProduct>> = flow {
    val path = "users/$userId/favorites"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("favorites")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(FavoriteProduct::class.java, ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          handleFlowError(error, OperationType.LIST, path, userId)
          throw error
        }
    )
  }

  suspend fun toggleFavorite(product: OrganicProduct, currentlyFavorited: Boolean): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("favorites").document(product.id)
    return try {
      if (currentlyFavorited) {
        docRef.delete().await()
      } else {
        docRef
          .set(
            mapOf(
              "userId" to uid,
              "productId" to product.id,
              "productName" to product.name.take(100),
              "createdAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
      Result.failure(e)
    }
  }
}
