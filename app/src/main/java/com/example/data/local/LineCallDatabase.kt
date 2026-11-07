package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CallRecord
import com.example.data.model.Contact

@Database(entities = [Contact::class, CallRecord::class], version = 1, exportSchema = false)
abstract class LineCallDatabase : RoomDatabase() {
  abstract fun contactDao(): ContactDao
  abstract fun callRecordDao(): CallRecordDao

  companion object {
    @Volatile
    private var INSTANCE: LineCallDatabase? = null

    fun getDatabase(context: Context): LineCallDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          LineCallDatabase::class.java,
          "linecall_database"
        ).fallbackToDestructiveMigration().build()
        INSTANCE = instance
        instance
      }
    }
  }
}
