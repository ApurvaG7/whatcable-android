package com.whatcable.android.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargingSampleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sample: ChargingSample)

    @Query("SELECT * FROM charging_samples WHERE timestamp > :since ORDER BY timestamp ASC")
    fun getSamplesSince(since: Long): Flow<List<ChargingSample>>

    @Query("SELECT * FROM charging_samples ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ChargingSample>

    @Query("DELETE FROM charging_samples WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
