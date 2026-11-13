package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class OrganicProduct(
  val id: String,
  val name: String,
  val category: String,
  val region: String,
  val altitude: String,
  val unitLabel: String,
  val priceInr: Double,
  val description: String,
  val harvestNote: String,
  val isBestSeller: Boolean = false,
)

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String? = null,
  val deliveryAddress: String = "",
  val preferredRegion: String = "Kumaon",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CartItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val productId: String = "",
  val productName: String = "",
  val category: String = "",
  val unitLabel: String = "",
  val unitPrice: Double = 0.0,
  val quantity: Long = 1L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Order(
  @DocumentId val id: String = "",
  val userId: String = "",
  val orderNumber: String = "",
  val itemSummary: String = "",
  val itemCount: Long = 0L,
  val totalAmount: Double = 0.0,
  val deliveryAddress: String = "",
  val status: String = "placed",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class FavoriteProduct(
  @DocumentId val id: String = "",
  val userId: String = "",
  val productId: String = "",
  val productName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)
