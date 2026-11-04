package com.arrowpuzzle.game.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StringsTest {
    private val placeholder = Regex("%(\\d+\\$)?[sd]")

    private fun placeholders(text: String): List<String> =
        placeholder.findAll(text).map { it.value }.sorted().toList()

    @Test
    fun everyLanguageHasATable() {
        for (language in AppLanguage.translated) {
            assertTrue("$language has no table", translationTables.containsKey(language))
        }
    }

    @Test
    fun everyTableIsComplete() {
        val reference = referenceStrings.keys
        for ((language, table) in translationTables) {
            val missing = reference - table.keys
            val extra = table.keys - reference
            assertTrue("$language is missing $missing", missing.isEmpty())
            assertTrue("$language has unknown keys $extra", extra.isEmpty())
        }
    }

    @Test
    fun placeholdersMatchTheEnglishReference() {
        for ((language, table) in translationTables) {
            for ((key, text) in table) {
                val expected = placeholders(referenceStrings.getValue(key))
                assertEquals("$language/$key placeholders", expected, placeholders(text))
            }
        }
    }

    @Test
    fun numbersUseTheGroupingSeparatorOfTheAppLanguage() {
        assertEquals("3,195", AppStrings(AppLanguage.ENGLISH).number(3195))
        assertEquals("3.195", AppStrings(AppLanguage.GERMAN).number(3195))
        assertEquals("1.234.567", AppStrings(AppLanguage.GERMAN).number(1_234_567))
        assertEquals("480", AppStrings(AppLanguage.GERMAN).number(480))
        for (language in AppLanguage.translated) {
            val digits = AppStrings(language).number(1_234_567).filter { it.isDigit() }
            assertEquals("$language keeps all digits", "1234567", digits)
        }
    }

    @Test
    fun formattedStringsDoNotThrow() {
        for (language in AppLanguage.translated) {
            val strings = AppStrings(language)
            strings.f("resume", "12", 34)
            strings.f("dev_progress", 1, 2000)
            strings.f("arrows_open", 240, 2)
            strings.f("level_info", 2000, 240)
            strings.f("seconds", 90)
        }
    }
}
