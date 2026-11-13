package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CallRecord
import com.example.data.model.Contact

@Database(entities = [Contact::class, CallRecord::class], version = 2, exportSchema = false)
abstract class LineCallDatabase : RoomDatabase() {
  abstract fun contactDao(): ContactDao
  abstract fun callRecordDao(): CallRecordDao

  companion object {
    private val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE contacts ADD COLUMN email TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE contacts ADD COLUMN registeredUserId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE contacts ADD COLUMN isRegistered INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE contacts ADD COLUMN presence TEXT NOT NULL DEFAULT 'offline'")
      }
    }

    @Volatile
    private var INSTANCE: LineCallDatabase? = null

    fun getDatabase(context: Context): LineCallDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          LineCallDatabase::class.java,
          "linecall_database"
        ).addMigrations(MIGRATION_1_2).fallbackToDestructiveMigration().build()
        INSTANCE = instance
        instance
      }
    }
  }
}
