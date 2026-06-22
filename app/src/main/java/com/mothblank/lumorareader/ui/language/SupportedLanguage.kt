package com.mothblank.lumorareader.ui.language

import java.util.Locale

/**
 * Enum representing the languages that LumoraReader supports.
 * Each entry maps to an Android [Locale] tag that must match a corresponding
 * `res/values-xx` folder so that string resources are resolved correctly.
 */
enum class SupportedLanguage(val tag: String, val displayName: String) {
    English("en", "English"),
    Czech("cs", "Čeština"),
    Portuguese("pt-BR", "Português (Brasil)"),
    Russian("ru", "Русский"),
    Slovak("sk", "Slovenčina"),
    Spanish("es", "Español"),
    French("fr", "Français"),
    ChineseSimplified("zh-CN", "简体中文");

    val locale: Locale
        get() = Locale.forLanguageTag(tag)

    companion object {
        fun fromTag(tag: String?): SupportedLanguage =
            entries.firstOrNull { it.tag == tag } ?: English
    }
}