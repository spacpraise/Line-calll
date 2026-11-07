package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CallRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface CallRecordDao {
  @Query("SELECT * FROM call_records ORDER BY timestamp DESC")
  fun getAllCallRecords(): Flow<List<CallRecord>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCallRecord(record: CallRecord): Long

  @Query("DELETE FROM call_records WHERE id = :id")
  suspend fun deleteCallRecord(id: Long)

  @Query("DELETE FROM call_records")
  suspend fun clearAllCallRecords()
}
