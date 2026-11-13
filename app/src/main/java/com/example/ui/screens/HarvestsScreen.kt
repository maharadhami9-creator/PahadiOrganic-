package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.catalog.HarvestCatalog
import com.example.data.model.CartItem
import com.example.data.model.FavoriteProduct
import com.example.data.model.OrganicProduct
import com.example.ui.UiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HarvestsScreen(
  userDisplayName: String,
  preferredRegion: String,
  products: List<OrganicProduct>,
  selectedCategory: String,
  searchQuery: String,
  cartState: UiState<List<CartItem>>,
  favoritesState: UiState<List<FavoriteProduct>>,
  selectedProduct: OrganicProduct?,
  onSelectCategory: (String) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onOpenProduct: (OrganicProduct) -> Unit,
  onCloseProduct: () -> Unit,
  onAddToCart: (OrganicProduct) -> Unit,
  onToggleFavorite: (OrganicProduct, Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val cartQuantities =
    when (cartState) {
      is UiState.Success -> cartState.data.associate { it.productId to it.quantity.toInt() }
      else -> emptyMap()
    }
  val favoriteIds =
    when (favoritesState) {
      is UiState.Success -> favoritesState.data.map { it.productId }.toSet()
      else -> emptySet()
    }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.greeting_namaste, userDisplayName.substringBefore(" ")),
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = stringResource(R.string.harvests_subheading),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Filled.Terrain,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = preferredRegion.ifBlank { "Kumaon" },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchQueryChange,
          placeholder = { Text(stringResource(R.string.search_harvests_hint)) },
          leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { onSearchQueryChange("") }) {
                Icon(
                  imageVector = Icons.Filled.Close,
                  contentDescription = stringResource(R.string.cd_clear_search),
                )
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              unfocusedContainerColor =
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
          modifier = Modifier.fillMaxWidth().testTag("search_harvests_input"),
        )
      }
    }

    // Seasonal Harvest Hero Banner
    item {
      Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(152.dp),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          Image(
            painter = painterResource(id = R.drawable.img_hero_pahad),
            contentDescription = stringResource(R.string.cd_hero_banner),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(
                  Brush.horizontalGradient(
                    colors =
                      listOf(
                        Color(0xFF10241A).copy(alpha = 0.90f),
                        Color(0xFF10241A).copy(alpha = 0.55f),
                        Color.Transparent,
                      )
                  )
                )
          )
          Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.Center,
          ) {
            Surface(
              shape = RoundedCornerShape(50),
              color = MaterialTheme.colorScheme.tertiary,
            ) {
              Text(
                text = stringResource(R.string.banner_season_tag),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = stringResource(R.string.banner_harvest_title),
              style = MaterialTheme.typography.headlineSmall,
              color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = stringResource(R.string.banner_harvest_desc),
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.88f),
            )
          }
        }
      }
    }

    // Category Chips
    item {
      LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(HarvestCatalog.categories) { category ->
          val isSelected = category == selectedCategory
          FilterChip(
            selected = isSelected,
            onClick = { onSelectCategory(category) },
            label = { Text(category) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
              ),
            modifier = Modifier.testTag("category_chip_${category.lowercase().replace(" ", "_")}"),
          )
        }
      }
    }

    if (products.isEmpty()) {
      item {
        Surface(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
          Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(
              imageVector = Icons.Filled.Eco,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = stringResource(R.string.empty_search_title),
              style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = stringResource(R.string.empty_search_desc),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      items(products, key = { it.id }) { product ->
        val isFav = favoriteIds.contains(product.id)
        val inCartQty = cartQuantities[product.id] ?: 0
        OrganicProductCard(
          product = product,
          isFavorite = isFav,
          inCartQuantity = inCartQty,
          onClick = { onOpenProduct(product) },
          onToggleFavorite = { onToggleFavorite(product, isFav) },
          onAddToCart = { onAddToCart(product) },
          modifier = Modifier.padding(horizontal = 20.dp),
        )
      }
    }
  }

  if (selectedProduct != null) {
    val isFav = favoriteIds.contains(selectedProduct.id)
    ModalBottomSheet(
      onDismissRequest = onCloseProduct,
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Text(
              text = "${selectedProduct.region} • ${selectedProduct.altitude}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }
          IconButton(
            onClick = { onToggleFavorite(selectedProduct, isFav) },
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(
              imageVector =
                if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
              contentDescription = stringResource(R.string.cd_toggle_favorite),
              tint =
                if (isFav) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = selectedProduct.name,
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "${selectedProduct.category} • ${selectedProduct.unitLabel}",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = selectedProduct.description,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.Eco,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = selectedProduct.harvestNote,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = stringResource(R.string.price_label),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = formatInr(selectedProduct.priceInr),
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Button(
            onClick = {
              onAddToCart(selectedProduct)
              onCloseProduct()
            },
            modifier = Modifier.height(50.dp),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.AddShoppingCart,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_to_basket))
          }
        }
      }
    }
  }
}

@Composable
private fun OrganicProductCard(
  product: OrganicProduct,
  isFavorite: Boolean,
  inCartQuantity: Int,
  onClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  onAddToCart: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .clickable(onClick = onClick)
        .testTag("product_card_${product.id}"),
    shape = RoundedCornerShape(20.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Text(
              text = "${product.altitude} • ${product.region}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
          if (product.isBestSeller) {
            Surface(
              shape = RoundedCornerShape(50),
              color = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
              Text(
                text = stringResource(R.string.badge_bestseller),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }
        }

        IconButton(
          onClick = onToggleFavorite,
          modifier =
            Modifier.minimumInteractiveComponentSize().testTag("favorite_btn_${product.id}"),
        ) {
          Icon(
            imageVector =
              if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(R.string.cd_toggle_favorite),
            tint =
              if (isFavorite) MaterialTheme.colorScheme.tertiary
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = product.name,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = product.description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = formatInr(product.priceInr),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = product.unitLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        FilledTonalButton(
          onClick = onAddToCart,
          modifier = Modifier.height(48.dp).testTag("add_to_cart_${product.id}"),
          shape = RoundedCornerShape(14.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.AddShoppingCart,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text =
              if (inCartQuantity > 0) {
                stringResource(R.string.in_basket_count, inCartQuantity)
              } else {
                stringResource(R.string.add_to_basket)
              }
          )
        }
      }
    }
  }
}

fun formatInr(amount: Double): String {
  return String.format(Locale.US, "₹%.0f", amount)
}
