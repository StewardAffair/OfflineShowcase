package ua.ofkkt.offlineshowcase.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import ua.ofkkt.offlineshowcase.domain.Product

/** Вкладений об'єкт: його поля стають стовпцями таблиці products (sup_title, sup_city). */
data class Supplier(
    val title: String = "",
    val city: String = "",
)

/** Слайд 5: Entity описує таблицю. */
@Entity(
    tableName = "products",
    indices = [Index(value = ["product_name"])],
)
data class ProductEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "product_name") val name: String,
    val price: Double,
    val cachedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = 0L,
    @Embedded(prefix = "sup_") val supplier: Supplier = Supplier(),
) {
    /** @Ignore — поле є в класі, але його немає в таблиці. */
    @Ignore
    var uiHint: String = ""
}

fun ProductEntity.toDomain() = Product(
    id = id,
    name = name,
    price = price,
    updatedAt = updatedAt,
    supplierTitle = supplier.title,
    supplierCity = supplier.city,
)

fun Product.toEntity(cachedAt: Long) = ProductEntity(
    id = id,
    name = name,
    price = price,
    cachedAt = cachedAt,
    updatedAt = updatedAt,
    supplier = Supplier(supplierTitle, supplierCity),
)

/**
 * Слайд 16 («Ігнорування OnConflictStrategy»): ПОГАНА Entity.
 * Ключ автогенерується, тому REPLACE ніколи не спрацьовує — кожен refresh додає дублікати.
 */
@Entity(tableName = "naive_products")
data class NaiveProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val remoteId: Int,
    val name: String,
)
