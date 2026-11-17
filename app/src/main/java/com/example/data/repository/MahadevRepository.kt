package com.example.data.repository

import android.content.Context
import android.os.Build
import com.example.R
import com.example.data.firebase.AdminCredentials
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.DefaultStoreCatalog
import com.example.data.model.GrainItem
import com.example.data.model.LoginLog
import com.example.data.model.StockStatus
import com.example.data.model.StoreConfig
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MahadevRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeGrains(): Flow<List<GrainItem>> {
        return db.collection("grains")
            .orderBy("name", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(GrainItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
    }

    fun observeWatchlistIds(userId: String): Flow<Set<String>> {
        require(userId.isNotBlank()) { "userId must not be blank" }
        return db.collection("users")
            .document(userId)
            .collection("watchlist")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.id }.toSet()
            }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        require(userId.isNotBlank()) { "userId must not be blank" }
        return db.collection("users")
            .document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
    }

    fun observeLoginLogs(): Flow<List<LoginLog>> {
        return db.collection("loginLogs")
            .orderBy("loginAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(LoginLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
    }

    fun observeStoreConfig(): Flow<StoreConfig> {
        return db.collection("storeConfig")
            .document("appShare")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(StoreConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: StoreConfig()
                } else {
                    StoreConfig()
                }
            }
    }

    suspend fun registerCustomerMobileLogin(
        mobileNumber: String,
        displayName: String
    ): Result<Unit> {
        val cleanMobile = mobileNumber.trim()
        val cleanName = displayName.trim().ifBlank { "ગ્રાહક ($cleanMobile)" }.take(80)
        if (!cleanMobile.matches(Regex("^[0-9]{10}$"))) {
            return Result.failure(IllegalArgumentException("કૃપા કરીને 10 અંકનો સાચો મોબાઈલ નંબર દાખલ કરો"))
        }

        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val userEmail = auth.currentUser?.email?.take(120)
        val userDocPath = "users/$uid"

        return try {
            val userDocRef = db.collection("users").document(uid)
            val existingSnapshot = userDocRef.get().await()
            if (existingSnapshot.exists()) {
                val updates = buildMap<String, Any> {
                    put("mobileNumber", cleanMobile)
                    put("displayName", cleanName)
                    if (!userEmail.isNullOrBlank()) put("email", userEmail)
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                userDocRef.update(updates).await()
            } else {
                val payload = buildMap<String, Any> {
                    put("userId", uid)
                    put("mobileNumber", cleanMobile)
                    put("displayName", cleanName)
                    if (!userEmail.isNullOrBlank()) put("email", userEmail)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                userDocRef.set(payload).await()
            }

            val logRef = db.collection("loginLogs").document()
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(100)
            val logPayload = buildMap<String, Any> {
                put("userId", uid)
                put("mobileNumber", cleanMobile)
                put("customerName", cleanName)
                if (!userEmail.isNullOrBlank()) put("email", userEmail)
                put("loginRole", "customer")
                if (deviceName.isNotBlank()) put("deviceInfo", deviceName)
                put("loginAt", FieldValue.serverTimestamp())
            }
            logRef.set(logPayload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDocPath)
            Result.failure(e)
        }
    }

    suspend fun verifyAndUnlockAdmin(
        mobileNumber: String,
        password: String
    ): Result<String> {
        val cleanMobile = mobileNumber.trim()
        if (!AdminCredentials.isValid(cleanMobile, password)) {
            return Result.failure(IllegalArgumentException("મોબાઈલ નંબર અથવા પાસવર્ડ ખોટો છે"))
        }

        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val adminLabel = AdminCredentials.adminLabelFor(cleanMobile)
        val adminDocPath = "admins/$uid"
        return try {
            val adminRef = db.collection("admins").document(uid)
            val existing = adminRef.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "mobileNumber" to cleanMobile,
                    "adminLabel" to adminLabel,
                    "passcodeHash" to AdminCredentials.sha256Hex(password),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                adminRef.set(payload).await()
            }

            // Record admin login in loginLogs as well
            val userEmail = auth.currentUser?.email?.take(120)
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(100)
            val logPayload = buildMap<String, Any> {
                put("userId", uid)
                put("mobileNumber", cleanMobile)
                put("customerName", adminLabel)
                if (!userEmail.isNullOrBlank()) put("email", userEmail)
                put("loginRole", "admin")
                if (deviceName.isNotBlank()) put("deviceInfo", deviceName)
                put("loginAt", FieldValue.serverTimestamp())
            }
            db.collection("loginLogs").document().set(logPayload).await()

            Result.success(adminLabel)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, adminDocPath)
            Result.failure(e)
        }
    }

    suspend fun toggleWatchlist(grainId: String, currentlySaved: Boolean): Result<Unit> {
        if (grainId.isBlank()) return Result.failure(IllegalArgumentException("અમાન્ય ઉત્પાદન ID"))
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val path = "users/$uid/watchlist/$grainId"
        return try {
            val docRef = db.collection("users").document(uid).collection("watchlist").document(grainId)
            if (currentlySaved) {
                docRef.delete().await()
            } else {
                docRef.set(
                    mapOf(
                        "grainId" to grainId,
                        "addedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun addGrain(
        name: String,
        category: String,
        pricePerKg: Double,
        priceUnit: String,
        weight: String,
        stockStatus: StockStatus,
        notes: String?,
        iconKey: String,
        imageBase64: String?
    ): Result<Unit> {
        val cleanName = name.trim()
        val cleanCategory = category.trim().ifBlank { "અનાજ" }.take(50)
        val cleanUnit = priceUnit.trim().ifBlank { "Kg" }.take(30)
        val cleanWeight = weight.trim().ifBlank { "50 Kg (બેગ)" }.take(60)
        val cleanNotes = notes?.trim()?.take(300)?.takeIf { it.isNotEmpty() }
        val cleanImage = imageBase64?.takeIf { it.length <= 100_000 }

        if (cleanName.isEmpty() || cleanName.length > 80) {
            return Result.failure(IllegalArgumentException("અનાજનું નામ 1 થી 80 અક્ષરનું હોવું જરૂરી છે"))
        }
        if (pricePerKg < 0.0 || pricePerKg > 1_000_000.0) {
            return Result.failure(IllegalArgumentException("ભાવ 0 થી 10,00,000 વચ્ચે હોવો જોઈએ"))
        }

        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("grains").document()
        return try {
            val payload = buildMap<String, Any> {
                put("name", cleanName)
                put("category", cleanCategory)
                put("pricePerKg", pricePerKg)
                put("priceUnit", cleanUnit)
                put("weight", cleanWeight)
                put("stockStatus", stockStatus.label)
                if (cleanNotes != null) put("notes", cleanNotes)
                put("iconKey", iconKey.take(30).ifBlank { "other" })
                if (cleanImage != null) put("imageBase64", cleanImage)
                put("updatedBy", uid)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateGrainFull(
        grainId: String,
        name: String,
        category: String,
        pricePerKg: Double,
        priceUnit: String,
        weight: String,
        stockStatus: StockStatus,
        notes: String?,
        iconKey: String,
        imageBase64: String?
    ): Result<Unit> {
        if (grainId.isBlank()) return Result.failure(IllegalArgumentException("અમાન્ય અનાજ ID"))
        val cleanName = name.trim()
        if (cleanName.isEmpty() || cleanName.length > 80) {
            return Result.failure(IllegalArgumentException("અનાજનું નામ 1 થી 80 અક્ષરનું હોવું જરૂરી છે"))
        }
        if (pricePerKg < 0.0 || pricePerKg > 1_000_000.0) {
            return Result.failure(IllegalArgumentException("ભાવ 0 થી 10,00,000 વચ્ચે હોવો જોઈએ"))
        }

        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("grains").document(grainId)
        return try {
            val updates = buildMap<String, Any> {
                put("name", cleanName)
                put("category", category.trim().ifBlank { "અનાજ" }.take(50))
                put("pricePerKg", pricePerKg)
                put("priceUnit", priceUnit.trim().ifBlank { "Kg" }.take(30))
                put("weight", weight.trim().ifBlank { "50 Kg (બેગ)" }.take(60))
                put("stockStatus", stockStatus.label)
                put("notes", (notes?.trim() ?: "").take(300))
                put("iconKey", iconKey.take(30).ifBlank { "other" })
                if (imageBase64 != null && imageBase64.length <= 100_000) {
                    put("imageBase64", imageBase64)
                }
                put("updatedBy", uid)
                put("updatedAt", FieldValue.serverTimestamp())
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateGrainPrice(grainId: String, newPrice: Double): Result<Unit> {
        if (grainId.isBlank()) return Result.failure(IllegalArgumentException("અમાન્ય ID"))
        if (newPrice < 0.0 || newPrice > 1_000_000.0) {
            return Result.failure(IllegalArgumentException("માન્ય ભાવ દાખલ કરો"))
        }
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("grains").document(grainId)
        return try {
            docRef.update(
                mapOf(
                    "pricePerKg" to newPrice,
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateGrainWeight(grainId: String, newWeight: String): Result<Unit> {
        val cleanWeight = newWeight.trim()
        if (grainId.isBlank() || cleanWeight.isEmpty() || cleanWeight.length > 60) {
            return Result.failure(IllegalArgumentException("માન્ય વજન દાખલ કરો (1-60 અક્ષર)"))
        }
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("grains").document(grainId)
        return try {
            docRef.update(
                mapOf(
                    "weight" to cleanWeight,
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateGrainStock(grainId: String, status: StockStatus): Result<Unit> {
        if (grainId.isBlank()) return Result.failure(IllegalArgumentException("અમાન્ય ID"))
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("grains").document(grainId)
        return try {
            docRef.update(
                mapOf(
                    "stockStatus" to status.label,
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteGrain(grainId: String): Result<Unit> {
        if (grainId.isBlank()) return Result.failure(IllegalArgumentException("અમાન્ય ID"))
        try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("grains").document(grainId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun seedDefaultGrains(): Result<Int> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val batch = db.batch()
            DefaultStoreCatalog.defaultSeeds.forEach { seed ->
                val ref = db.collection("grains").document(seed.docId)
                val payload = mapOf(
                    "name" to seed.name,
                    "category" to seed.category,
                    "pricePerKg" to seed.pricePerKg,
                    "priceUnit" to seed.priceUnit,
                    "weight" to seed.weight,
                    "stockStatus" to seed.stockStatus,
                    "notes" to seed.notes,
                    "iconKey" to seed.iconKey,
                    "updatedBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(ref, payload)
            }
            batch.commit().await()
            Result.success(DefaultStoreCatalog.defaultSeeds.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "grains")
            Result.failure(e)
        }
    }

    suspend fun saveStoreConfig(downloadUrl: String, headline: String): Result<Unit> {
        val cleanUrl = StoreConfig.normalizeApkDownloadUrl(downloadUrl)
        val cleanHeadline = headline.trim().ifBlank { StoreConfig.DEFAULT_HEADLINE }.take(100)
        if (cleanUrl.length < 10 || cleanUrl.length > 500 ||
            (!cleanUrl.startsWith("https://") && !cleanUrl.startsWith("http://"))
        ) {
            return Result.failure(
                IllegalArgumentException("કૃપા કરીને https:// અથવા http:// થી શરૂ થતી માન્ય Google Drive / APK લિંક લખો")
            )
        }
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val path = "storeConfig/appShare"
        return try {
            db.collection("storeConfig").document("appShare").set(
                mapOf(
                    "downloadUrl" to cleanUrl,
                    "headline" to cleanHeadline,
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
