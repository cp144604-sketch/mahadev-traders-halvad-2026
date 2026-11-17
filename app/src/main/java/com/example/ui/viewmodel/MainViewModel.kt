package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AdminCredentials
import com.example.data.model.GrainItem
import com.example.data.model.LoginLog
import com.example.data.model.StockStatus
import com.example.data.model.StoreConfig
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.preferences.LocalSessionPrefs
import com.example.data.preferences.SessionPreferences
import com.example.data.repository.MahadevRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab { HOME, MY_LIST, ADMIN }

enum class AdminSubScreen {
    DASHBOARD,
    ADD_EDIT_GRAIN,
    UPDATE_PRICE,
    UPDATE_WEIGHT,
    UPDATE_STOCK,
    DELETE_GRAIN,
    GRAIN_LIST,
    LOGIN_LOGS,
    QR_SHARE
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: MahadevRepository,
    private val sessionPrefs: SessionPreferences,
    val currentUserId: String
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(AppTab.HOME)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _selectedGrainForDetail = MutableStateFlow<GrainItem?>(null)
    val selectedGrainForDetail: StateFlow<GrainItem?> = _selectedGrainForDetail.asStateFlow()

    private val _adminSubScreen = MutableStateFlow(AdminSubScreen.DASHBOARD)
    val adminSubScreen: StateFlow<AdminSubScreen> = _adminSubScreen.asStateFlow()

    private val _editingGrain = MutableStateFlow<GrainItem?>(null)
    val editingGrain: StateFlow<GrainItem?> = _editingGrain.asStateFlow()

    private val _showCustomerLoginGate = MutableStateFlow(false)
    val showCustomerLoginGate: StateFlow<Boolean> = _showCustomerLoginGate.asStateFlow()

    private val _showHomeQrScreen = MutableStateFlow(false)
    val showHomeQrScreen: StateFlow<Boolean> = _showHomeQrScreen.asStateFlow()

    private val _isAdminUnlocked = MutableStateFlow(false)
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    private val _adminLabel = MutableStateFlow("Admin 1")
    val adminLabel: StateFlow<String> = _adminLabel.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _adminLoginError = MutableStateFlow<String?>(null)
    val adminLoginError: StateFlow<String?> = _adminLoginError.asStateFlow()

    private val _customerLoginError = MutableStateFlow<String?>(null)
    val customerLoginError: StateFlow<String?> = _customerLoginError.asStateFlow()

    // Search queries across every section
    private val _homeSearchQuery = MutableStateFlow("")
    val homeSearchQuery: StateFlow<String> = _homeSearchQuery.asStateFlow()

    private val _homeCategory = MutableStateFlow("બધા")
    val homeCategory: StateFlow<String> = _homeCategory.asStateFlow()

    private val _myListSearchQuery = MutableStateFlow("")
    val myListSearchQuery: StateFlow<String> = _myListSearchQuery.asStateFlow()

    private val _adminSearchQuery = MutableStateFlow("")
    val adminSearchQuery: StateFlow<String> = _adminSearchQuery.asStateFlow()

    private val _loginLogSearchQuery = MutableStateFlow("")
    val loginLogSearchQuery: StateFlow<String> = _loginLogSearchQuery.asStateFlow()

    private val _loginLogRoleFilter = MutableStateFlow("બધા")
    val loginLogRoleFilter: StateFlow<String> = _loginLogRoleFilter.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    val localPrefs: StateFlow<LocalSessionPrefs> = sessionPrefs.prefsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = LocalSessionPrefs()
        )

    val grainsState: StateFlow<UiState<List<GrainItem>>> = repository.observeGrains()
        .map<List<GrainItem>, UiState<List<GrainItem>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing grains", rootError)
            emit(UiState.Error(rootError.message ?: "અનાજ યાદી લોડ કરવામાં ભૂલ આવી"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val watchlistIds: StateFlow<Set<String>> = repository.observeWatchlistIds(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing watchlist", firestoreError ?: error)
            emit(emptySet())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptySet()
        )

    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing user profile", firestoreError ?: error)
            emit(UiState.Error(firestoreError?.message ?: error.message ?: "પ્રોફાઈલ લોડ થઈ શકી નહીં"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    // Admin login logs stream — ONLY subscribed when admin is authenticated & unlocked!
    val loginLogsState: StateFlow<UiState<List<LoginLog>>> = _isAdminUnlocked
        .flatMapLatest { unlocked ->
            if (!unlocked) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeLoginLogs()
                    .map<List<LoginLog>, UiState<List<LoginLog>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Error observing login logs", rootError)
                        emit(UiState.Error(rootError.message ?: "લોગીન વિગતો લોડ થઈ શકી નહીં"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val storeConfig: StateFlow<StoreConfig> = repository.observeStoreConfig()
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing storeConfig", firestoreError ?: error)
            emit(StoreConfig())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = StoreConfig()
        )

    // Keep detail view synced live if Admin updates an item while customer is viewing it!
    val liveDetailGrain: StateFlow<GrainItem?> = combine(
        _selectedGrainForDetail,
        grainsState
    ) { selected, state ->
        if (selected == null) null
        else if (state is UiState.Success) state.data.firstOrNull { it.id == selected.id } ?: selected
        else selected
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null
    )

    init {
        viewModelScope.launch {
            sessionPrefs.prefsFlow.collect { prefs ->
                if (prefs.rememberAdmin && prefs.savedAdminMobile in AdminCredentials.authorizedNumbers) {
                    _isAdminUnlocked.value = true
                    _adminLabel.value = prefs.adminLabel.ifBlank { AdminCredentials.adminLabelFor(prefs.savedAdminMobile) }
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
        _selectedGrainForDetail.value = null
        if (tab != AppTab.ADMIN) {
            _adminSubScreen.value = AdminSubScreen.DASHBOARD
            _editingGrain.value = null
        }
    }

    fun openGrainDetail(grain: GrainItem) {
        _selectedGrainForDetail.value = grain
    }

    fun closeGrainDetail() {
        _selectedGrainForDetail.value = null
    }

    fun openCustomerLoginGate() {
        _customerLoginError.value = null
        _showCustomerLoginGate.value = true
    }

    fun closeCustomerLoginGate() {
        _customerLoginError.value = null
        _showCustomerLoginGate.value = false
    }

    fun openHomeQrScreen() {
        _showHomeQrScreen.value = true
    }

    fun closeHomeQrScreen() {
        _showHomeQrScreen.value = false
    }

    fun notifyMessage(message: String) {
        _snackbarEvents.tryEmit(message)
    }

    fun saveStoreConfig(downloadUrl: String, headline: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveStoreConfig(downloadUrl, headline)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.tryEmit("QR ડાઉનલોડ લિંક અપડેટ થઈ ગઈ!")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "લિંક સેવ થઈ શકી નહીં")
            }
        }
    }

    fun updateHomeSearch(query: String) {
        _homeSearchQuery.value = query
    }

    fun updateHomeCategory(category: String) {
        _homeCategory.value = category
    }

    fun updateMyListSearch(query: String) {
        _myListSearchQuery.value = query
    }

    fun updateAdminSearch(query: String) {
        _adminSearchQuery.value = query
    }

    fun updateLoginLogSearch(query: String) {
        _loginLogSearchQuery.value = query
    }

    fun updateLoginLogRoleFilter(filter: String) {
        _loginLogRoleFilter.value = filter
    }

    fun navigateAdminSubScreen(subScreen: AdminSubScreen, editGrain: GrainItem? = null) {
        _adminSearchQuery.value = ""
        _loginLogSearchQuery.value = ""
        _editingGrain.value = editGrain
        _adminSubScreen.value = subScreen
    }

    fun registerCustomerMobile(mobileNumber: String, displayName: String) {
        val cleanMobile = mobileNumber.trim()
        if (!cleanMobile.matches(Regex("^[0-9]{10}$"))) {
            _customerLoginError.value = "કૃપા કરીને 10 અંકનો સાચો મોબાઈલ નંબર લખો"
            return
        }
        val cleanName = displayName.trim().ifBlank { "ગ્રાહક ($cleanMobile)" }
        viewModelScope.launch {
            _isBusy.value = true
            _customerLoginError.value = null
            val result = repository.registerCustomerMobileLogin(cleanMobile, cleanName)
            _isBusy.value = false
            result.onSuccess {
                sessionPrefs.saveCustomerSession(cleanMobile, cleanName)
                _showCustomerLoginGate.value = false
                _snackbarEvents.tryEmit("સ્વાગત છે, $cleanName! લાઈવ ભાવ તૈયાર છે.")
            }.onFailure { err ->
                _customerLoginError.value = err.localizedMessage ?: "લોગીન સેવ કરવામાં ભૂલ આવી"
            }
        }
    }

    fun loginAsAdmin(mobileNumber: String, password: String, rememberMe: Boolean) {
        val cleanMobile = mobileNumber.trim()
        if (cleanMobile !in AdminCredentials.authorizedNumbers) {
            _adminLoginError.value = "ફક્ત અધિકૃત એડમિન મોબાઈલ નંબર જ માન્ય છે"
            return
        }
        if (password.isBlank()) {
            _adminLoginError.value = "કૃપા કરીને પાસવર્ડ દાખલ કરો"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            _adminLoginError.value = null
            val result = repository.verifyAndUnlockAdmin(cleanMobile, password)
            _isBusy.value = false
            result.onSuccess { label ->
                _adminLabel.value = label
                _isAdminUnlocked.value = true
                _adminSubScreen.value = AdminSubScreen.DASHBOARD
                sessionPrefs.saveAdminSession(rememberMe, cleanMobile, label)
                _snackbarEvents.tryEmit("એડમિન પેનલ અનલોક થઈ ગઈ છે")
            }.onFailure { err ->
                _adminLoginError.value = err.localizedMessage ?: "મોબાઈલ નંબર અથવા પાસવર્ડ ખોટો છે"
            }
        }
    }

    fun lockAdminPanel() {
        viewModelScope.launch {
            sessionPrefs.clearAdminSession()
            _isAdminUnlocked.value = false
            _adminSubScreen.value = AdminSubScreen.DASHBOARD
            _editingGrain.value = null
            _snackbarEvents.tryEmit("એડમિન લોગઆઉટ થઈ ગયું")
        }
    }

    fun toggleWatchlist(grainId: String, currentlySaved: Boolean) {
        viewModelScope.launch {
            val result = repository.toggleWatchlist(grainId, currentlySaved)
            result.onSuccess {
                val msg = if (currentlySaved) "મારી યાદીમાંથી દૂર કર્યું" else "મારી યાદીમાં ઉમેર્યું"
                _snackbarEvents.tryEmit(msg)
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "યાદી અપડેટ થઈ શકી નહીં")
            }
        }
    }

    fun saveGrain(
        existingId: String?,
        name: String,
        category: String,
        priceText: String,
        priceUnit: String,
        weight: String,
        stockStatus: StockStatus,
        notes: String,
        iconKey: String,
        imageBase64: String?,
        onSaved: () -> Unit
    ) {
        val parsedPrice = priceText.trim().toDoubleOrNull()
        if (name.trim().isEmpty()) {
            _snackbarEvents.tryEmit("કૃપા કરીને અનાજનું નામ લખો")
            return
        }
        if (parsedPrice == null || parsedPrice < 0.0) {
            _snackbarEvents.tryEmit("કૃપા કરીને સાચો ભાવ (₹) લખો")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = if (existingId.isNullOrBlank()) {
                repository.addGrain(
                    name = name,
                    category = category,
                    pricePerKg = parsedPrice,
                    priceUnit = priceUnit,
                    weight = weight,
                    stockStatus = stockStatus,
                    notes = notes,
                    iconKey = iconKey,
                    imageBase64 = imageBase64
                )
            } else {
                repository.updateGrainFull(
                    grainId = existingId,
                    name = name,
                    category = category,
                    pricePerKg = parsedPrice,
                    priceUnit = priceUnit,
                    weight = weight,
                    stockStatus = stockStatus,
                    notes = notes,
                    iconKey = iconKey,
                    imageBase64 = imageBase64
                )
            }
            _isBusy.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit(if (existingId.isNullOrBlank()) "નવું અનાજ સેવ થઈ ગયું!" else "અનાજની વિગતો અપડેટ થઈ ગઈ!")
                onSaved()
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "સેવ કરવામાં ભૂલ આવી")
            }
        }
    }

    fun quickUpdatePrice(grainId: String, priceText: String) {
        val price = priceText.trim().toDoubleOrNull()
        if (price == null || price < 0.0) {
            _snackbarEvents.tryEmit("કૃપા કરીને સાચો ભાવ દાખલ કરો")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateGrainPrice(grainId, price)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.tryEmit("ભાવ તરત જ અપડેટ થઈ ગયો!")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "ભાવ અપડેટ થઈ શક્યો નહીં")
            }
        }
    }

    fun quickUpdateWeight(grainId: String, weight: String) {
        if (weight.trim().isEmpty()) {
            _snackbarEvents.tryEmit("કૃપા કરીને વજન લખો")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateGrainWeight(grainId, weight)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.tryEmit("વજન અપડેટ થઈ ગયું!")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "વજન અપડેટ થઈ શક્યું નહીં")
            }
        }
    }

    fun quickUpdateStock(grainId: String, status: StockStatus) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateGrainStock(grainId, status)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.tryEmit("સ્ટોક સ્ટેટસ અપડેટ થઈ ગયું!")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "સ્ટોક અપડેટ થઈ શક્યો નહીં")
            }
        }
    }

    fun deleteGrain(grainId: String, grainName: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.deleteGrain(grainId)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.tryEmit("$grainName યાદીમાંથી ડિલીટ થયું")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "ડિલીટ કરવામાં ભૂલ આવી")
            }
        }
    }

    fun seedDefaultStoreCatalog() {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.seedDefaultGrains()
            _isBusy.value = false
            res.onSuccess { count ->
                _snackbarEvents.tryEmit("$count પ્રારંભિક અનાજ ઉમેરાઈ ગયા!")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "પ્રારંભિક યાદી ઉમેરવામાં ભૂલ આવી")
            }
        }
    }

    suspend fun clearLocalPrefsBeforeSignOut() {
        sessionPrefs.clearAllOnSignOut()
    }

    private companion object {
        const val TAG = "MainViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
