package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.onGoogleSignInClicked
import com.example.data.repository.PahadRepository
import com.example.ui.PahadViewModel
import com.example.ui.UiState
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BasketOrdersScreen
import com.example.ui.screens.HarvestsScreen
import com.example.ui.screens.PantryProfileScreen
import com.example.ui.theme.PahadOrganicTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      PahadOrganicTheme {
        AppNavigation()
      }
    }
  }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val context = LocalContext.current
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val currentUser by
    auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

  LaunchedEffect(Unit) {
    if (auth.currentUser == null) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = {},
        onUnauthenticated = {},
        scope = this,
      )
    }
  }

  val user = currentUser
  if (user == null) {
    AuthScreen(onAuthSuccess = {})
  } else {
    key(user.uid) {
      MainFeatureScreen(user = user)
    }
  }
}

@Composable
fun MainFeatureScreen(user: FirebaseUser) {
  val currentUserId = user.uid
  val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "Pahad Member"
  val email = user.email

  val viewModel: PahadViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            PahadViewModel(
              repository = PahadRepository(db = db, auth = Firebase.auth),
              currentUserId = currentUserId,
              initialDisplayName = displayName,
              initialEmail = email,
            )
          }
        },
    )

  val products by viewModel.filteredProducts.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedProduct by viewModel.selectedProduct.collectAsStateWithLifecycle()
  val profileState by viewModel.profileState.collectAsStateWithLifecycle()
  val cartState by viewModel.cartState.collectAsStateWithLifecycle()
  val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
  val favoritesState by viewModel.favoritesState.collectAsStateWithLifecycle()
  val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(bannerMessage) {
    val msg = bannerMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearBannerMessage()
    }
  }

  var activeTab by rememberSaveable { mutableIntStateOf(0) }
  val cartCount =
    when (val state = cartState) {
      is UiState.Success -> state.data.sumOf { it.quantity.toInt() }
      else -> 0
    }
  val loadedProfile = (profileState as? UiState.Success)?.data
  val activeDisplayName = loadedProfile?.displayName?.ifBlank { displayName } ?: displayName
  val preferredRegion = loadedProfile?.preferredRegion ?: "Kumaon"
  val savedAddress = loadedProfile?.deliveryAddress ?: ""

  val navItems =
    listOf(
      Triple(stringResource(R.string.nav_harvests), Icons.Filled.Eco, Icons.Outlined.Eco),
      Triple(
        stringResource(R.string.nav_basket),
        Icons.Filled.ShoppingBasket,
        Icons.Outlined.ShoppingBasket,
      ),
      Triple(stringResource(R.string.nav_pantry), Icons.Filled.Person, Icons.Outlined.Person),
    )

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 600.dp

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (!useNavRail) {
          NavigationBar(windowInsets = WindowInsets.navigationBars) {
            navItems.forEachIndexed { index, (label, filledIcon, outlinedIcon) ->
              NavigationBarItem(
                selected = activeTab == index,
                onClick = { activeTab = index },
                icon = {
                  if (index == 1 && cartCount > 0) {
                    BadgedBox(badge = { Badge { Text(cartCount.toString()) } }) {
                      Icon(
                        imageVector = if (activeTab == index) filledIcon else outlinedIcon,
                        contentDescription = label,
                      )
                    }
                  } else {
                    Icon(
                      imageVector = if (activeTab == index) filledIcon else outlinedIcon,
                      contentDescription = label,
                    )
                  }
                },
                label = { Text(label) },
                modifier = Modifier.testTag("nav_tab_$index"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useNavRail) {
          NavigationRail {
            navItems.forEachIndexed { index, (label, filledIcon, outlinedIcon) ->
              NavigationRailItem(
                selected = activeTab == index,
                onClick = { activeTab = index },
                icon = {
                  if (index == 1 && cartCount > 0) {
                    BadgedBox(badge = { Badge { Text(cartCount.toString()) } }) {
                      Icon(
                        imageVector = if (activeTab == index) filledIcon else outlinedIcon,
                        contentDescription = label,
                      )
                    }
                  } else {
                    Icon(
                      imageVector = if (activeTab == index) filledIcon else outlinedIcon,
                      contentDescription = label,
                    )
                  }
                },
                label = { Text(label) },
              )
            }
          }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
          Box(modifier = Modifier.fillMaxSize().widthIn(max = 720.dp)) {
            when (activeTab) {
              0 ->
                HarvestsScreen(
                  userDisplayName = activeDisplayName,
                  preferredRegion = preferredRegion,
                  products = products,
                  selectedCategory = selectedCategory,
                  searchQuery = searchQuery,
                  cartState = cartState,
                  favoritesState = favoritesState,
                  selectedProduct = selectedProduct,
                  onSelectCategory = viewModel::selectCategory,
                  onSearchQueryChange = viewModel::updateSearchQuery,
                  onOpenProduct = viewModel::openProductDetail,
                  onCloseProduct = viewModel::closeProductDetail,
                  onAddToCart = viewModel::addToCart,
                  onToggleFavorite = viewModel::toggleFavorite,
                )
              1 ->
                BasketOrdersScreen(
                  cartState = cartState,
                  ordersState = ordersState,
                  savedAddress = savedAddress,
                  isBusy = isBusy,
                  onUpdateQuantity = viewModel::updateCartQuantity,
                  onRemoveItem = viewModel::removeCartItem,
                  onPlaceOrder = viewModel::placeOrder,
                  onCancelOrder = viewModel::cancelOrder,
                  onBrowseHarvests = { activeTab = 0 },
                )
              2 ->
                PantryProfileScreen(
                  userDisplayName = activeDisplayName,
                  userEmail = email,
                  profileState = profileState,
                  ordersState = ordersState,
                  favoritesState = favoritesState,
                  isBusy = isBusy,
                  onSaveProfile = viewModel::saveProfile,
                  onAddToCart = viewModel::addToCart,
                )
            }
          }
        }
      }
    }
  }
}
