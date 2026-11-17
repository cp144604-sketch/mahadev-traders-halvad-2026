package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class StockStatus(val label: String) {
    AVAILABLE("ઉપલબ્ધ"),
    LOW("ઓછો સ્ટોક"),
    OUT("સ્ટોક નથી");

    companion object {
        fun fromLabel(raw: String?): StockStatus =
            entries.firstOrNull { it.label == raw } ?: AVAILABLE
    }
}

enum class GrainIconKey(
    val key: String,
    val labelGu: String,
    val defaultCategory: String
) {
    WHEAT("wheat", "ઘઉં", "અનાજ"),
    RICE("rice", "ચોખા", "અનાજ"),
    BAJRA("bajra", "બાજરી", "અનાજ"),
    JOWAR("jowar", "જુવાર", "અનાજ"),
    CHANA("chana", "ચણા", "કઠોળ"),
    DAL("dal", "દાળ", "દાળ"),
    MOONG("moong", "મગ", "કઠોળ"),
    MAIZE("maize", "મકાઈ", "અનાજ"),
    OIL("oil", "તેલ", "તેલીબિયાં"),
    SPICES("spices", "મસાલા", "કરીયાણું"),
    SUGAR("sugar", "ખાંડ/ગોળ", "કરીયાણું"),
    OTHER("other", "અન્ય", "કરીયાણું");

    companion object {
        fun fromKey(raw: String?): GrainIconKey =
            entries.firstOrNull { it.key == raw } ?: OTHER
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mobileNumber: String = "",
    val displayName: String = "",
    val email: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WatchlistItem(
    @DocumentId val id: String = "",
    val grainId: String = "",
    @ServerTimestamp val addedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GrainItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = "અનાજ",
    val pricePerKg: Double = 0.0,
    val priceUnit: String = "Kg",
    val weight: String = "50 Kg (બેગ)",
    val stockStatus: String = StockStatus.AVAILABLE.label,
    val notes: String? = null,
    val iconKey: String = GrainIconKey.OTHER.key,
    val imageBase64: String? = null,
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun stockEnum(): StockStatus = StockStatus.fromLabel(stockStatus)
    fun iconEnum(): GrainIconKey = GrainIconKey.fromKey(iconKey)
}

@Keep
@IgnoreExtraProperties
data class LoginLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mobileNumber: String = "",
    val customerName: String = "",
    val email: String? = null,
    val loginRole: String = "customer",
    val deviceInfo: String? = null,
    @ServerTimestamp val loginAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mobileNumber: String = "",
    val adminLabel: String = "",
    val passcodeHash: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StoreConfig(
    @DocumentId val id: String = "",
    val downloadUrl: String = DEFAULT_DOWNLOAD_URL,
    val headline: String = DEFAULT_HEADLINE,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        const val DEFAULT_DOWNLOAD_URL = ""
        const val DEFAULT_HEADLINE = "મહાદેવ ટ્રેડર્સ અનાજ ભાવ એપ ડાઉનલોડ કરો"

        private val driveFilePattern = Regex("""drive\.google\.com/file/d/([a-zA-Z0-9_-]+)""")
        private val driveIdParamPattern = Regex("""[?&]id=([a-zA-Z0-9_-]+)""")

        fun normalizeApkDownloadUrl(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return ""

            if (trimmed.contains("drive.google.com", ignoreCase = true)) {
                val fileId = driveFilePattern.find(trimmed)?.groupValues?.getOrNull(1)
                    ?: driveIdParamPattern.find(trimmed)?.groupValues?.getOrNull(1)
                if (!fileId.isNullOrBlank()) {
                    return "https://drive.google.com/uc?export=download&id=$fileId"
                }
            }

            if (trimmed.contains("dropbox.com", ignoreCase = true) && trimmed.contains("dl=0")) {
                return trimmed.replace("dl=0", "dl=1")
            }

            return trimmed
        }
    }
}

data class GrainSeedSpec(
    val docId: String,
    val name: String,
    val category: String,
    val pricePerKg: Double,
    val priceUnit: String,
    val weight: String,
    val stockStatus: String,
    val iconKey: String,
    val notes: String
)

object DefaultStoreCatalog {
    val categories = listOf("બધા", "અનાજ", "કઠોળ", "દાળ", "તેલીબિયાં", "કરીયાણું")
    val editableCategories = listOf("અનાજ", "કઠોળ", "દાળ", "તેલીબિયાં", "કરીયાણું")
    val unitOptions = listOf("Kg", "20 Kg", "લિટર", "પેકેટ", "નંગ")

    val defaultSeeds = listOf(
        GrainSeedSpec("grain_wheat", "ઘઉં", "અનાજ", 32.0, "Kg", "50 Kg (બેગ)", StockStatus.AVAILABLE.label, GrainIconKey.WHEAT.key, "શરબતી / લોકવન ઘઉં — હોલસેલ તથા રીટેઈલ"),
        GrainSeedSpec("grain_rice", "ચોખા", "અનાજ", 58.0, "Kg", "25 Kg (બેગ)", StockStatus.AVAILABLE.label, GrainIconKey.RICE.key, "પ્રીમિયમ બાસમતી અને કોલમ ચોખા"),
        GrainSeedSpec("grain_bajra", "બાજરી", "અનાજ", 40.0, "Kg", "50 Kg (બેગ)", StockStatus.AVAILABLE.label, GrainIconKey.BAJRA.key, "દેશી હળવદ બાજરી"),
        GrainSeedSpec("grain_jowar", "જુવાર", "અનાજ", 38.0, "Kg", "50 Kg (બેગ)", StockStatus.AVAILABLE.label, GrainIconKey.JOWAR.key, "સફેદ જુવાર — સ્વચ્છ અને ચાળેલી"),
        GrainSeedSpec("grain_chana", "ચણા", "કઠોળ", 72.0, "Kg", "50 Kg (બેગ)", StockStatus.AVAILABLE.label, GrainIconKey.CHANA.key, "દેશી ચણા — ઉત્તમ ગુણવત્તા"),
        GrainSeedSpec("grain_toor_dal", "તુવેર દાળ", "દાળ", 120.0, "Kg", "30 Kg (બેગ)", StockStatus.AVAILABLE.label, GrainIconKey.DAL.key, "દેશી તુવેર દાળ — પૉલિશ વિનાની"),
        GrainSeedSpec("grain_moong", "મગ", "કઠોળ", 98.0, "Kg", "50 Kg (બેગ)", StockStatus.AVAILABLE.label, GrainIconKey.MOONG.key, "લીલા દેશી મગ"),
        GrainSeedSpec("grain_maize", "મકાઈ", "અનાજ", 30.0, "Kg", "50 Kg (બેગ)", StockStatus.AVAILABLE.label, GrainIconKey.MAIZE.key, "પીળી મકાઈ")
    )
}

fun formatTimestampGu(timestamp: Timestamp?): String {
    val safeTimestamp = timestamp ?: return "હમણાં જ"
    val formatter = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
    return formatter.format(safeTimestamp.toDate())
}

fun formatPriceRupees(price: Double): String {
    return if (price % 1.0 == 0.0) {
        price.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", price)
    }
}
