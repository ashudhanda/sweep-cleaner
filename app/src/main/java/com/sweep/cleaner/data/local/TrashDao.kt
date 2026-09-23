package com.sweep.cleaner.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash_items ORDER BY deletedTimestamp DESC")
    fun getAllTrashItems(): Flow<List<TrashEntity>>

    @Query("SELECT * FROM trash_items WHERE expiryTimestamp <= :cutoffTimestamp")
    suspend fun getExpiredTrashItems(cutoffTimestamp: Long): List<TrashEntity>

    @Query("SELECT * FROM trash_items WHERE id = :id LIMIT 1")
    suspend fun getTrashItemById(id: String): TrashEntity?

    @Query("SELECT SUM(sizeBytes) FROM trash_items")
    fun getTotalTrashSizeBytes(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM trash_items")
    fun getTrashItemCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashItem(item: TrashEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TrashEntity>)

    @Query("DELETE FROM trash_items WHERE id = :id")
    suspend fun deleteTrashItemById(id: String)

    @Delete
    suspend fun deleteTrashItems(items: List<TrashEntity>)

    @Query("DELETE FROM trash_items")
    suspend fun deleteAll()
}
