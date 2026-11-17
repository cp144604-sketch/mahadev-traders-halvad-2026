package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.signOutFirebase
import com.example.data.model.UiState
import com.example.data.preferences.SessionPreferences
import com.example.data.repository.MahadevRepository
import com.example.ui.screens.AddEditGrainScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminGrainListScreen
import com.example.ui.screens.AdminLoginScreen
import com.example.ui.screens.BrandedSplashScreen
import com.example.ui.screens.CustomerMobileLoginScreen
import com.example.ui.screens.DeleteGrainScreen
import com.example.ui.screens.GoogleAuthScreen
import com.example.ui.screens.HomeCatalogScreen
import com.example.ui.screens.LoginLogsScreen
import com.example.ui.screens.MyListScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.QrShareScreen
import com.example.ui.screens.QuickPriceUpdateScreen
import com.example.ui.screens.QuickStockUpdateScreen
import com.example.ui.screens.QuickWeightUpdateScreen
import com.example.ui.theme.BrandCrimson
import com.example.ui.theme.MahadevTradersTheme
import com.example.ui.viewmodel.AdminSubScreen
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MahadevTradersTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var splashActive by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
        delay(900L)
        splashActive = false
    }

    if (splashActive) {
        BrandedSplashScreen()
        return
    }

    val user = currentUser
    if (user == null) {
        GoogleAuthScreen(
            onAuthSuccess = {}
        )
    } else {
        key(user.uid) {
            MainAppContent(currentUserId = user.uid)
        }
    }
}

@Composable
private fun MainAppContent(currentUserId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MainViewModel(
                    repository = MahadevRepository(db),
                    sessionPrefs = SessionPreferences(app.applicationContext),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val liveDetailGrain by viewModel.liveDetailGrain.collectAsStateWithLifecycle()
    val adminSubScreen by viewModel.adminSubScreen.collectAsStateWithLifecycle()
    val editingGrain by viewModel.editingGrain.collectAsStateWithLifecycle()
    val showCustomerLoginGate by viewModel.showCustomerLoginGate.collectAsStateWithLifecycle()
    val showHomeQrScreen by viewModel.showHomeQrScreen.collectAsStateWithLifecycle()
    val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsStateWithLifecycle()
    val adminLabel by viewModel.adminLabel.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val adminLoginError by viewModel.adminLoginError.collectAsStateWithLifecycle()
    val customerLoginError by viewModel.customerLoginError.collectAsStateWithLifecycle()

    val grainsState by viewModel.grainsState.collectAsStateWithLifecycle()
    val watchlistIds by viewModel.watchlistIds.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val loginLogsState by viewModel.loginLogsState.collectAsStateWithLifecycle()
    val storeConfig by viewModel.storeConfig.collectAsStateWithLifecycle()
    val localPrefs by viewModel.localPrefs.collectAsStateWithLifecycle()

    val homeSearchQuery by viewModel.homeSearchQuery.collectAsStateWithLifecycle()
    val homeCategory by viewModel.homeCategory.collectAsStateWithLifecycle()
    val myListSearchQuery by viewModel.myListSearchQuery.collectAsStateWithLifecycle()
    val adminSearchQuery by viewModel.adminSearchQuery.collectAsStateWithLifecycle()
    val loginLogSearchQuery by viewModel.loginLogSearchQuery.collectAsStateWithLifecycle()
    val loginLogRoleFilter by viewModel.loginLogRoleFilter.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val performFullSignOut: () -> Unit = {
        signOutFirebase(
            credentialManager = credentialManager,
            scope = scope,
            beforeSignOut = { viewModel.clearLocalPrefsBeforeSignOut() }
        )
    }

    // Loading state while checking whether customer mobile profile already exists
    if (userProfileState is UiState.Loading && !showCustomerLoginGate && selectedTab != AppTab.ADMIN) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandCrimson)
        }
        return
    }

    val userProfile = (userProfileState as? UiState.Success)?.data
    val needsCustomerMobile = (userProfileState is UiState.Success && userProfile == null)

    if ((needsCustomerMobile || showCustomerLoginGate) && selectedTab != AppTab.ADMIN) {
        CustomerMobileLoginScreen(
            initialMobile = userProfile?.mobileNumber ?: localPrefs.lastCustomerMobile,
            initialName = userProfile?.displayName ?: localPrefs.lastCustomerName,
            isBusy = isBusy,
            errorMessage = customerLoginError,
            canDismiss = userProfile != null && showCustomerLoginGate,
            onSubmitMobile = { mobile, name ->
                viewModel.registerCustomerMobile(mobile, name)
            },
            onAdminShortcut = {
                viewModel.closeCustomerLoginGate()
                viewModel.selectTab(AppTab.ADMIN)
            },
            onDismiss = { viewModel.closeCustomerLoginGate() },
            onSignOutGoogle = performFullSignOut
        )
        return
    }

    if (showHomeQrScreen) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            QrShareScreen(
                storeConfig = storeConfig,
                isAdmin = isAdminUnlocked,
                isBusy = isBusy,
                onSaveConfig = viewModel::saveStoreConfig,
                onNotify = viewModel::notifyMessage,
                onBack = { viewModel.closeHomeQrScreen() },
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    // Full-screen Detail or Admin Sub-screen flows (with BackHandler inside each screen)
    if (liveDetailGrain != null) {
        val grain = liveDetailGrain!!
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            ProductDetailScreen(
                grain = grain,
                isSaved = grain.id in watchlistIds,
                onToggleWatchlist = {
                    viewModel.toggleWatchlist(grain.id, grain.id in watchlistIds)
                },
                onBack = { viewModel.closeGrainDetail() },
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    if (selectedTab == AppTab.ADMIN && isAdminUnlocked && adminSubScreen != AdminSubScreen.DASHBOARD) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            val subModifier = Modifier.padding(innerPadding)
            when (adminSubScreen) {
                AdminSubScreen.ADD_EDIT_GRAIN -> AddEditGrainScreen(
                    editingGrain = editingGrain,
                    isBusy = isBusy,
                    onSave = { id, name, cat, price, unit, weight, stock, notes, iconKey, img ->
                        viewModel.saveGrain(
                            existingId = id,
                            name = name,
                            category = cat,
                            priceText = price,
                            priceUnit = unit,
                            weight = weight,
                            stockStatus = stock,
                            notes = notes,
                            iconKey = iconKey,
                            imageBase64 = img,
                            onSaved = {
                                viewModel.navigateAdminSubScreen(AdminSubScreen.DASHBOARD)
                            }
                        )
                    },
                    onCancel = { viewModel.navigateAdminSubScreen(AdminSubScreen.DASHBOARD) },
                    modifier = subModifier
                )

                AdminSubScreen.UPDATE_PRICE -> QuickPriceUpdateScreen(
                    grainsState = grainsState,
                    searchQuery = adminSearchQuery,
                    isBusy = isBusy,
                    onSearchChange = viewModel::updateAdminSearch,
                    onUpdatePrice = viewModel::quickUpdatePrice,
                    onBack = { viewModel.navigateAdminSubScreen(AdminSubScreen.DASHBOARD) },
                    modifier = subModifier
                )

                AdminSubScreen.UPDATE_WEIGHT -> QuickWeightUpdateScreen(
                    grainsState = grainsState,
                    searchQuery = adminSearchQuery,
                    isBusy = isBusy,
                    onSearchChange = viewModel::updateAdminSearch,
                    onUpdateWeight = viewModel::quickUpdateWeight,
                    onBack = { viewModel.navigateAdminSubScreen(AdminSubScreen.DASHBOARD) },
                    modifier = subModifier
                )

                AdminSubScreen.UPDATE_STOCK -> QuickStockUpdateScreen(
                    grainsState = grainsState,
                    searchQuery = adminSearchQuery,
                    isBusy = isBusy,
                    onSearchChange = viewModel::updateAdminSearch,
                    onUpdateStock = viewModel::quickUpdateStock,
                    onBack = { viewModel.navigateAdminSubScreen(AdminSubScreen.DASHBOARD) },
                    modifier = subModifier
                )

                AdminSubScreen.DELETE_GRAIN -> DeleteGrainScreen(
                    grainsState = grainsState,
                    searchQuery = adminSearchQuery,
                    isBusy = isBusy,
                    onSearchChange = viewModel::updateAdminSearch,
                    onDeleteGrain = viewModel::deleteGrain,
                    onBack = { viewModel.navigateAdminSubScreen(AdminSubScreen.DASHBOARD) },
                    modifier = subModifier
                )

                AdminSubScreen.GRAIN_LIST -> AdminGrainListScreen(
                    grainsState = grainsState,
                    searchQuery = adminSearchQuery,
                    onSearchChange = viewModel::updateAdminSearch,
                    onEditGrain = { grain ->
                        viewModel.navigateAdminSubScreen(AdminSubScreen.ADD_EDIT_GRAIN, editGrain = grain)
                    },
                    onBack = { viewModel.navigateAdminSubScreen(AdminSubScreen.DASHBOARD) },
                    modifier = subModifier
                )

                AdminSubScreen.LOGIN_LOGS -> LoginLogsScreen(
                    logsState = loginLogsState,
                    searchQuery = loginLogSearchQuery,
                    roleFilter = loginLogRoleFilter,
                    onSearchChange = viewModel::updateLoginLogSearch,
                    onRoleFilterChange = viewModel::updateLoginLogRoleFilter,
                    onBack = { viewModel.navigateAdminSubScreen(AdminSubScreen.DASHBOARD) },
                    modifier = subModifier
                )

                AdminSubScreen.QR_SHARE -> QrShareScreen(
                    storeConfig = storeConfig,
                    isAdmin = true,
                    isBusy = isBusy,
                    onSaveConfig = viewModel::saveStoreConfig,
                    onNotify = viewModel::notifyMessage,
                    onBack = { viewModel.navigateAdminSubScreen(AdminSubScreen.DASHBOARD) },
                    modifier = subModifier
                )

                AdminSubScreen.DASHBOARD -> Unit
            }
        }
        return
    }

    val grainsCount = (grainsState as? UiState.Success)?.data?.size ?: 0
    val loginsCount = (loginLogsState as? UiState.Success)?.data?.size ?: 0

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        if (useRail) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tabTitle(tab)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tabTitle(tab),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = BrandCrimson,
                                    selectedTextColor = BrandCrimson,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }

                    MainTabBody(
                        selectedTab = selectedTab,
                        viewModel = viewModel,
                        grainsState = grainsState,
                        watchlistIds = watchlistIds,
                        userProfile = userProfile,
                        homeSearchQuery = homeSearchQuery,
                        homeCategory = homeCategory,
                        myListSearchQuery = myListSearchQuery,
                        isAdminUnlocked = isAdminUnlocked,
                        adminLabel = adminLabel,
                        grainsCount = grainsCount,
                        loginsCount = loginsCount,
                        localPrefsSavedMobile = localPrefs.savedAdminMobile,
                        localPrefsRemember = localPrefs.rememberAdmin,
                        isBusy = isBusy,
                        adminLoginError = adminLoginError,
                        onSignOutGoogle = performFullSignOut,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tabTitle(tab)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tabTitle(tab),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BrandCrimson,
                                    selectedTextColor = BrandCrimson,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                MainTabBody(
                    selectedTab = selectedTab,
                    viewModel = viewModel,
                    grainsState = grainsState,
                    watchlistIds = watchlistIds,
                    userProfile = userProfile,
                    homeSearchQuery = homeSearchQuery,
                    homeCategory = homeCategory,
                    myListSearchQuery = myListSearchQuery,
                    isAdminUnlocked = isAdminUnlocked,
                    adminLabel = adminLabel,
                    grainsCount = grainsCount,
                    loginsCount = loginsCount,
                    localPrefsSavedMobile = localPrefs.savedAdminMobile,
                    localPrefsRemember = localPrefs.rememberAdmin,
                    isBusy = isBusy,
                    adminLoginError = adminLoginError,
                    onSignOutGoogle = performFullSignOut,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun MainTabBody(
    selectedTab: AppTab,
    viewModel: MainViewModel,
    grainsState: UiState<List<com.example.data.model.GrainItem>>,
    watchlistIds: Set<String>,
    userProfile: com.example.data.model.UserProfile?,
    homeSearchQuery: String,
    homeCategory: String,
    myListSearchQuery: String,
    isAdminUnlocked: Boolean,
    adminLabel: String,
    grainsCount: Int,
    loginsCount: Int,
    localPrefsSavedMobile: String,
    localPrefsRemember: Boolean,
    isBusy: Boolean,
    adminLoginError: String?,
    onSignOutGoogle: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (selectedTab) {
        AppTab.HOME -> HomeCatalogScreen(
            grainsState = grainsState,
            watchlistIds = watchlistIds,
            userProfile = userProfile,
            searchQuery = homeSearchQuery,
            selectedCategory = homeCategory,
            onSearchChange = viewModel::updateHomeSearch,
            onCategoryChange = viewModel::updateHomeCategory,
            onGrainClick = viewModel::openGrainDetail,
            onToggleWatchlist = { grain ->
                viewModel.toggleWatchlist(grain.id, grain.id in watchlistIds)
            },
            onOpenCustomerProfile = viewModel::openCustomerLoginGate,
            onOpenQrShare = viewModel::openHomeQrScreen,
            onSignOutGoogle = onSignOutGoogle,
            modifier = modifier
        )

        AppTab.MY_LIST -> MyListScreen(
            grainsState = grainsState,
            watchlistIds = watchlistIds,
            searchQuery = myListSearchQuery,
            onSearchChange = viewModel::updateMyListSearch,
            onGrainClick = viewModel::openGrainDetail,
            onToggleWatchlist = { grain ->
                viewModel.toggleWatchlist(grain.id, grain.id in watchlistIds)
            },
            modifier = modifier
        )

        AppTab.ADMIN -> if (!isAdminUnlocked) {
            AdminLoginScreen(
                initialMobile = localPrefsSavedMobile,
                initialRemember = localPrefsRemember,
                isBusy = isBusy,
                errorMessage = adminLoginError,
                onLoginClick = viewModel::loginAsAdmin,
                modifier = modifier
            )
        } else {
            AdminDashboardScreen(
                adminLabel = adminLabel,
                totalGrainsCount = grainsCount,
                totalLoginsCount = loginsCount,
                isBusy = isBusy,
                onNavigate = { sub -> viewModel.navigateAdminSubScreen(sub) },
                onSeedDefaults = viewModel::seedDefaultStoreCatalog,
                onLogoutAdmin = viewModel::lockAdminPanel,
                modifier = modifier
            )
        }
    }
}

private fun tabTitle(tab: AppTab): String = when (tab) {
    AppTab.HOME -> "હોમ"
    AppTab.MY_LIST -> "મારી યાદી"
    AppTab.ADMIN -> "એડમિન"
}

private fun tabIcon(tab: AppTab, selected: Boolean) = when (tab) {
    AppTab.HOME -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
    AppTab.MY_LIST -> if (selected) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder
    AppTab.ADMIN -> if (selected) Icons.Rounded.AdminPanelSettings else Icons.Outlined.AdminPanelSettings
}
