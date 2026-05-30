package com.whatcable.android.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CableTestDao {

    @Insert
    suspend fun insert(test: CableTest): Long

    /** All saved tests, best performer first. */
    @Query("SELECT * FROM cable_tests ORDER BY peakWatts DESC")
    fun getAllByPeak(): Flow<List<CableTest>>

    @Delete
    suspend fun delete(test: CableTest)

    @Query("DELETE FROM cable_tests")
    suspend fun deleteAll()
}
