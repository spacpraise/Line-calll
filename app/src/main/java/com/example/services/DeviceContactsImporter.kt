package com.example.services

import android.content.Context
import android.provider.ContactsContract
import com.example.data.model.Contact

/** Reads the phone address book only after Android's READ_CONTACTS permission is granted. */
object DeviceContactsImporter {
  fun read(context: Context, dialCode: String): List<Contact> {
    val resolver = context.contentResolver
    val emailsByContact = HashMap<Long, String>()
    resolver.query(
      ContactsContract.CommonDataKinds.Email.CONTENT_URI,
      arrayOf(ContactsContract.CommonDataKinds.Email.CONTACT_ID, ContactsContract.CommonDataKinds.Email.ADDRESS),
      null, null, null
    )?.use { cursor ->
      val idColumn = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
      val emailColumn = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
      while (cursor.moveToNext()) {
        val id = cursor.getLong(idColumn)
        val email = cursor.getString(emailColumn)?.trim()?.lowercase().orEmpty()
        if (email.isNotBlank() && !emailsByContact.containsKey(id)) emailsByContact[id] = email
      }
    }

    val result = LinkedHashMap<String, Contact>()
    resolver.query(
      ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
      arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
      null, null,
      ContactsContract.Contacts.DISPLAY_NAME + " COLLATE NOCASE ASC"
    )?.use { cursor ->
      val idColumn = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
      val nameColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
      val numberColumn = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
      while (cursor.moveToNext()) {
        val contactId = cursor.getLong(idColumn)
        val name = cursor.getString(nameColumn)?.trim().orEmpty().ifBlank { "Unknown contact" }
        val e164 = normalizeNumber(cursor.getString(numberColumn).orEmpty(), dialCode) ?: continue
        if (!result.containsKey(e164)) {
          result[e164] = Contact(
            name = name,
            phoneNumber = e164,
            avatarInitials = name.take(2).uppercase(),
            email = emailsByContact[contactId].orEmpty()
          )
        }
      }
    }
    return result.values.toList()
  }

  private fun normalizeNumber(raw: String, dialCode: String): String? {
    val trimmed = raw.trim()
    val digits = trimmed.filter(Char::isDigit)
    if (trimmed.startsWith("+")) return PhoneNumberFormatter.formatToE164("+", trimmed).takeIf(PhoneNumberFormatter::isValidE164)
    if (digits.isBlank()) return null
    val countryDigits = dialCode.filter(Char::isDigit)
    val normalized = when {
      countryDigits.isNotBlank() && digits.startsWith(countryDigits) -> "+$digits"
      countryDigits.isNotBlank() && digits.startsWith("0") -> "+$countryDigits" + digits.dropWhile { it == '0' }
      else -> PhoneNumberFormatter.formatToE164(dialCode, digits)
    }
    return normalized.takeIf(PhoneNumberFormatter::isValidE164)
  }
}
