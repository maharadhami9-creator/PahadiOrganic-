package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.catalog.HarvestCatalog
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PahadRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureAndUpdateUserProfile_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = PahadRepository(firestore, auth)

    val ensureResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.ensureUserProfile("Aarav Rawat", ALICE_EMAIL) }
    assertTrue("ensureUserProfile failed: ${ensureResult.exceptionOrNull()}", ensureResult.isSuccess)

    val updateResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.updateUserProfile("Aarav Rawat", "Mall Road, Almora", "Kumaon")
      }
    assertTrue("updateUserProfile failed: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)

    val profile = withTimeout(FLOW_TIMEOUT_MS) { repo.observeUserProfile(uid).first { it != null } }
    assertNotNull(profile)
    assertEquals("Mall Road, Almora", profile?.deliveryAddress)
    assertEquals("Kumaon", profile?.preferredRegion)
  }

  @Test
  fun addToCartAndPlaceOrder_authenticatedOwner_persistsOrderAndClearsCart() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = PahadRepository(firestore, auth)
    val honey = HarvestCatalog.products.first()

    val addResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.addToCart(honey) }
    assertTrue("addToCart failed: ${addResult.exceptionOrNull()}", addResult.isSuccess)

    val cartItems =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeCartItems(uid).first { list -> list.any { it.productId == honey.id } }
      }
    assertEquals(1, cartItems.size)
    assertEquals(1L, cartItems.first().quantity)

    val orderResult =
      withTimeout(DEFAULT_TIMEOUT_MS) { repo.placeOrder(cartItems, "Ranikhet Orchard, Almora") }
    assertTrue("placeOrder failed: ${orderResult.exceptionOrNull()}", orderResult.isSuccess)

    val orderId = orderResult.getOrThrow()
    val orders =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeOrders(uid).first { list -> list.any { it.id == orderId } }
      }
    assertEquals("placed", orders.first { it.id == orderId }.status)

    val cancelResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.cancelOrder(orderId) }
    assertTrue("cancelOrder failed: ${cancelResult.exceptionOrNull()}", cancelResult.isSuccess)
  }

  @Test
  fun toggleFavorite_authenticatedOwner_createsAndDeletesFavorite() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = PahadRepository(firestore, auth)
    val product = HarvestCatalog.products[2]

    val addFav = withTimeout(DEFAULT_TIMEOUT_MS) { repo.toggleFavorite(product, false) }
    assertTrue("toggleFavorite add failed: ${addFav.exceptionOrNull()}", addFav.isSuccess)

    val favorites =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeFavorites(uid).first { list -> list.any { it.productId == product.id } }
      }
    assertTrue(favorites.any { it.productId == product.id })

    val removeFav = withTimeout(DEFAULT_TIMEOUT_MS) { repo.toggleFavorite(product, true) }
    assertTrue("toggleFavorite remove failed: ${removeFav.exceptionOrNull()}", removeFav.isSuccess)
  }

  @Test
  fun observeCartItems_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = PahadRepository(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeCartItems("alice_unauth_check").first() }
      fail("Expected PERMISSION_DENIED for unauthenticated user")
    } catch (e: Exception) {
      val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
        "Expected nested FirebaseFirestoreException, got $e"
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }
  }

  @Test
  fun crossUserRead_authenticatedBobReadingAliceProfile_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = PahadRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.ensureUserProfile("Alice Rawat", ALICE_EMAIL).getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid).get().await()
      }
      fail("Expected PERMISSION_DENIED for cross-user profile read")
    } catch (e: Exception) {
      val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
        "Expected nested FirebaseFirestoreException, got $e"
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice_pahad@test.com"
    const val BOB_EMAIL = "bob_pahad@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
