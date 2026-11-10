package ua.ofkkt.offlineshowcase.domain

/**
 * Доменна модель. Нічого не знає про Room, SQLite чи мережу
 * (слайд 5: «Entity — це окремий клас від доменної моделі»).
 */
data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    /** Коли цей запис востаннє змінено (на сервері або локально). */
    val updatedAt: Long,
    val supplierTitle: String = "",
    val supplierCity: String = "",
)
