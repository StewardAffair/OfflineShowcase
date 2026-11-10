package ua.ofkkt.offlineshowcase.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Слайд 6: DAO — інтерфейс із анотаціями; реалізацію генерує Room (KSP). */
@Dao
interface ProductDao {

    /** Слайд 7: Flow — Room сам перевипускає список при кожній зміні таблиці. */
    @Query("SELECT * FROM products ORDER BY product_name ASC")
    fun getAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getById(productId: Int): ProductEntity?

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    /** Слайд 8: НЕ-suspend запит. Викликаний з UI-потоку — кине IllegalStateException. */
    @Query("SELECT * FROM products ORDER BY product_name ASC")
    fun getAllBlocking(): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    // Три стратегії для демо на слайдах 6 і 16
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAbort(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Delete
    suspend fun delete(product: ProductEntity)

    @Query("DELETE FROM products")
    suspend fun deleteAll()
}

@Dao
interface NaiveDao {
    @Query("SELECT * FROM naive_products ORDER BY remoteId, id")
    fun getAll(): Flow<List<NaiveProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<NaiveProductEntity>)

    @Query("DELETE FROM naive_products")
    suspend fun deleteAll()
}
