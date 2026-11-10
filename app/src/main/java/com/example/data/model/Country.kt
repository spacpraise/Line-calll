package com.example.data.model

data class Country(
  val name: String,
  val code: String,      // ISO code: "US", "GB", "NG", etc.
  val dialCode: String,  // "+1", "+44", "+234", etc.
  val flagEmoji: String,
  val exampleFormat: String = "123 456 7890"
)

object CountryList {
  val allCountries: List<Country> = listOf(
    Country("United States", "US", "+1", "🇺🇸", "555 123 4567"),
    Country("United Kingdom", "GB", "+44", "🇬🇧", "7911 123456"),
    Country("Nigeria", "NG", "+234", "🇳🇬", "803 123 4567"),
    Country("Canada", "CA", "+1", "🇨🇦", "416 123 4567"),
    Country("Germany", "DE", "+49", "🇩🇪", "151 12345678"),
    Country("France", "FR", "+33", "🇫🇷", "6 12 34 56 78"),
    Country("India", "IN", "+91", "🇮🇳", "98765 43210"),
    Country("Japan", "JP", "+81", "🇯🇵", "90 1234 5678"),
    Country("Australia", "AU", "+61", "🇦🇺", "412 345 678"),
    Country("Brazil", "BR", "+55", "🇧🇷", "11 91234 5678"),
    Country("South Africa", "ZA", "+27", "🇿🇦", "82 123 4567"),
    Country("Kenya", "KE", "+254", "🇰🇪", "712 345678"),
    Country("Ghana", "GH", "+233", "🇬🇭", "24 123 4567"),
    Country("United Arab Emirates", "AE", "+971", "🇦🇪", "50 123 4567"),
    Country("Singapore", "SG", "+65", "🇸🇬", "8123 4567")
  )

  val defaultCountry: Country = allCountries.first { it.code == "NG" } // Nigeria is the default calling region

  fun findByDialCode(dialCode: String): Country? {
    return allCountries.find { it.dialCode == dialCode }
  }
}
