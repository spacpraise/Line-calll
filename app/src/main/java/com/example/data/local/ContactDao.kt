package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
  @Query("SELECT * FROM contacts ORDER BY name ASC")
  fun getAllContacts(): Flow<List<Contact>>

  @Query("SELECT * FROM contacts WHERE name LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%' ORDER BY name ASC")
  fun searchContacts(query: String): Flow<List<Contact>>

  @Query("SELECT * FROM contacts")
  suspend fun getAllContactsSnapshot(): List<Contact>

  @Query("SELECT * FROM contacts WHERE phoneNumber = :phoneNumber LIMIT 1")
  suspend fun getContactByPhone(phoneNumber: String): Contact?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertContact(contact: Contact): Long

  @Update
  suspend fun updateContact(contact: Contact)

  @Delete
  suspend fun deleteContact(contact: Contact)
}
