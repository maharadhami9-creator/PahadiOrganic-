package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.ui.UiState
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun BasketOrdersScreen(
  cartState: UiState<List<CartItem>>,
  ordersState: UiState<List<Order>>,
  savedAddress: String,
  isBusy: Boolean,
  onUpdateQuantity: (String, Int) -> Unit,
  onRemoveItem: (String) -> Unit,
  onPlaceOrder: (List<CartItem>, String, () -> Unit) -> Unit,
  onCancelOrder: (String) -> Unit,
  onBrowseHarvests: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedSegment by rememberSaveable { mutableIntStateOf(0) }
  var deliveryAddress by rememberSaveable { mutableStateOf(savedAddress) }

  LaunchedEffect(savedAddress) {
    if (deliveryAddress.isBlank() && savedAddress.isNotBlank()) {
      deliveryAddress = savedAddress
    }
  }

  val cartCount =
    when (cartState) {
      is UiState.Success -> cartState.data.sumOf { it.quantity.toInt() }
      else -> 0
    }
  val orderCount =
    when (ordersState) {
      is UiState.Success -> ordersState.data.size
      else -> 0
    }

  Column(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
      Text(
        text = stringResource(R.string.basket_orders_title),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Spacer(modifier = Modifier.height(12.dp))
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
          selected = selectedSegment == 0,
          onClick = { selectedSegment = 0 },
          shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
          modifier = Modifier.testTag("segment_basket"),
        ) {
          Text(stringResource(R.string.tab_basket_count, cartCount))
        }
        SegmentedButton(
          selected = selectedSegment == 1,
          onClick = { selectedSegment = 1 },
          shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
          modifier = Modifier.testTag("segment_orders"),
        ) {
          Text(stringResource(R.string.tab_orders_count, orderCount))
        }
      }
    }

    if (selectedSegment == 0) {
      when (cartState) {
        is UiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
        is UiState.Error -> {
          Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
              text = cartState.message,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
        is UiState.Success -> {
          val items = cartState.data
          if (items.isEmpty()) {
            EmptyStateCard(
              icon = Icons.Filled.ShoppingBasket,
              title = stringResource(R.string.empty_basket_title),
              subtitle = stringResource(R.string.empty_basket_desc),
              actionLabel = stringResource(R.string.browse_harvests),
              onAction = onBrowseHarvests,
            )
          } else {
            val subtotal = items.sumOf { it.unitPrice * it.quantity }
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              items(items, key = { it.id.ifBlank { it.productId } }) { item ->
                val docId = item.id.ifBlank { item.productId }
                CartItemCard(
                  item = item,
                  onDecrement = { onUpdateQuantity(docId, item.quantity.toInt() - 1) },
                  onIncrement = { onUpdateQuantity(docId, item.quantity.toInt() + 1) },
                  onRemove = { onRemoveItem(docId) },
                )
              }

              item {
                Card(
                  modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 20.dp),
                  shape = RoundedCornerShape(20.dp),
                  colors =
                    CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                ) {
                  Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                      text = stringResource(R.string.delivery_details_title),
                      style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                      value = deliveryAddress,
                      onValueChange = { deliveryAddress = it },
                      label = { Text(stringResource(R.string.delivery_address_label)) },
                      placeholder = { Text(stringResource(R.string.delivery_address_hint)) },
                      minLines = 2,
                      maxLines = 3,
                      shape = RoundedCornerShape(14.dp),
                      modifier = Modifier.fillMaxWidth().testTag("checkout_address_input"),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Text(
                        text = stringResource(R.string.order_total_label),
                        style = MaterialTheme.typography.titleMedium,
                      )
                      Text(
                        text = formatInr(subtotal),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                      onClick = {
                        onPlaceOrder(items, deliveryAddress) { selectedSegment = 1 }
                      },
                      enabled = !isBusy && deliveryAddress.isNotBlank(),
                      modifier =
                        Modifier.fillMaxWidth().height(52.dp).testTag("place_order_button"),
                      shape = RoundedCornerShape(14.dp),
                    ) {
                      if (isBusy) {
                        CircularProgressIndicator(
                          modifier = Modifier.size(20.dp),
                          color = MaterialTheme.colorScheme.onPrimary,
                          strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                      }
                      Icon(
                        imageVector = Icons.Filled.LocalShipping,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(stringResource(R.string.place_organic_order))
                    }
                  }
                }
              }
            }
          }
        }
      }
    } else {
      when (ordersState) {
        is UiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
        is UiState.Error -> {
          Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
              text = ordersState.message,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
        is UiState.Success -> {
          val orders = ordersState.data
          if (orders.isEmpty()) {
            EmptyStateCard(
              icon = Icons.Outlined.ReceiptLong,
              title = stringResource(R.string.empty_orders_title),
              subtitle = stringResource(R.string.empty_orders_desc),
              actionLabel = stringResource(R.string.browse_harvests),
              onAction = onBrowseHarvests,
            )
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              items(orders, key = { it.id }) { order ->
                OrderCard(order = order, onCancelOrder = { onCancelOrder(order.id) })
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CartItemCard(
  item: CartItem,
  onDecrement: () -> Unit,
  onIncrement: () -> Unit,
  onRemove: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.productName,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "${item.unitLabel} • ${formatInr(item.unitPrice)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onDecrement,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(
            imageVector = Icons.Filled.Remove,
            contentDescription = stringResource(R.string.cd_decrease_quantity),
          )
        }
        Text(
          text = item.quantity.toString(),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(horizontal = 4.dp),
        )
        IconButton(
          onClick = onIncrement,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.cd_increase_quantity),
          )
        }
        IconButton(
          onClick = onRemove,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(
            imageVector = Icons.Filled.DeleteOutline,
            contentDescription = stringResource(R.string.cd_remove_item),
            tint = MaterialTheme.colorScheme.error,
          )
        }
      }
    }
  }
}

@Composable
private fun OrderCard(
  order: Order,
  onCancelOrder: () -> Unit,
) {
  val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US) }
  val timestamp = order.updatedAt ?: order.createdAt ?: Timestamp.now()
  val formattedDate = dateFormatter.format(timestamp.toDate())
  val isCancelled = order.status == "cancelled"

  Card(
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier = Modifier.fillMaxWidth().testTag("order_card_${order.id}"),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = order.orderNumber,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Surface(
          shape = RoundedCornerShape(50),
          color =
            if (isCancelled) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.primaryContainer,
        ) {
          Text(
            text = order.status.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelMedium,
            color =
              if (isCancelled) MaterialTheme.colorScheme.error
              else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = order.itemSummary,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "${order.deliveryAddress} • $formattedDate",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${order.itemCount} items • ${formatInr(order.totalAmount)}",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary,
        )
        if (order.status == "placed") {
          OutlinedButton(
            onClick = onCancelOrder,
            modifier = Modifier.height(48.dp).testTag("cancel_order_${order.id}"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Text(stringResource(R.string.cancel_order))
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyStateCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  actionLabel: String,
  onAction: () -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(44.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(onClick = onAction, modifier = Modifier.height(48.dp)) {
          Text(actionLabel)
        }
      }
    }
  }
}
