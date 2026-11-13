package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.signOut
import com.example.data.catalog.HarvestCatalog
import com.example.data.model.FavoriteProduct
import com.example.data.model.Order
import com.example.data.model.OrganicProduct
import com.example.data.model.UserProfile
import com.example.ui.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantryProfileScreen(
  userDisplayName: String,
  userEmail: String?,
  profileState: UiState<UserProfile?>,
  ordersState: UiState<List<Order>>,
  favoritesState: UiState<List<FavoriteProduct>>,
  isBusy: Boolean,
  onSaveProfile: (String, String, String) -> Unit,
  onAddToCart: (OrganicProduct) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = CredentialManager.create(context)

  val loadedProfile = (profileState as? UiState.Success)?.data
  var displayNameInput by rememberSaveable { mutableStateOf(userDisplayName) }
  var addressInput by rememberSaveable { mutableStateOf("") }
  var regionInput by rememberSaveable { mutableStateOf("Kumaon") }
  var hydratedFromCloud by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(loadedProfile) {
    if (loadedProfile != null && !hydratedFromCloud) {
      if (loadedProfile.displayName.isNotBlank()) displayNameInput = loadedProfile.displayName
      addressInput = loadedProfile.deliveryAddress
      if (loadedProfile.preferredRegion.isNotBlank()) regionInput = loadedProfile.preferredRegion
      hydratedFromCloud = true
    }
  }

  val ordersCount = (ordersState as? UiState.Success)?.data?.size ?: 0
  val favoritesList = (favoritesState as? UiState.Success)?.data ?: emptyList()

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Card(
        shape = RoundedCornerShape(22.dp),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = displayNameInput.trim().take(1).uppercase().ifBlank { "P" },
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onPrimary,
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = displayNameInput.ifBlank { userDisplayName },
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (!userEmail.isNullOrBlank()) {
              Text(
                text = userEmail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = stringResource(R.string.google_auth_verified),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        }
      }
    }

    // Stats Row
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        StatCard(
          label = stringResource(R.string.stat_orders),
          value = ordersCount.toString(),
          modifier = Modifier.weight(1f),
        )
        StatCard(
          label = stringResource(R.string.stat_saved_harvests),
          value = favoritesList.size.toString(),
          modifier = Modifier.weight(1f),
        )
      }
    }

    // Profile Preferences Form
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text(
            text = stringResource(R.string.pantry_preferences_title),
            style = MaterialTheme.typography.titleMedium,
          )
          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = displayNameInput,
            onValueChange = { displayNameInput = it },
            label = { Text(stringResource(R.string.label_display_name)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
          )
          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = addressInput,
            onValueChange = { addressInput = it },
            label = { Text(stringResource(R.string.label_saved_address)) },
            placeholder = { Text(stringResource(R.string.delivery_address_hint)) },
            minLines = 2,
            maxLines = 3,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("profile_address_input"),
          )

          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = stringResource(R.string.label_preferred_region),
            style = MaterialTheme.typography.labelLarge,
          )
          Spacer(modifier = Modifier.height(8.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            HarvestCatalog.regions.forEach { region ->
              FilterChip(
                selected = regionInput == region,
                onClick = { regionInput = region },
                label = { Text(region) },
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          Button(
            onClick = { onSaveProfile(displayNameInput, addressInput, regionInput) },
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_profile_button"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.CloudDone,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.save_profile_cta))
          }
        }
      }
    }

    // Saved Favorites
    if (favoritesList.isNotEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.saved_harvests_title),
                style = MaterialTheme.typography.titleMedium,
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            favoritesList.forEach { fav ->
              val catalogProduct = HarvestCatalog.products.find { it.id == fav.productId }
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = fav.productName, style = MaterialTheme.typography.bodyMedium)
                  if (catalogProduct != null) {
                    Text(
                      text = "${catalogProduct.region} • ${formatInr(catalogProduct.priceInr)}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
                if (catalogProduct != null) {
                  IconButton(
                    onClick = { onAddToCart(catalogProduct) },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.AddShoppingCart,
                      contentDescription = stringResource(R.string.add_to_basket),
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Sign Out
    item {
      OutlinedButton(
        onClick = { signOut(credentialManager = credentialManager, scope = coroutineScope) },
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("sign_out_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Logout,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.sign_out_cta))
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun StatCard(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
