package com.mothblank.lumorareader.presentation.reader

import com.mothblank.lumorareader.ui.theme.LumoraTheme
import java.util.Locale

/**
 * Pure quotation theme suggestion logic, independent from any reader lifecycle.
 * User-chosen themes remain the authority when a quote is edited.
 */
internal object QuoteThemeClassifier {
    // Enhanced auto-categorization with better keyword matching
    private val themeKeywords: Map<LumoraTheme, Set<String>> = mapOf(
        LumoraTheme.GOTH to setOf("dark", "darkness", "night", "shadow", "shadows", "goth", "gothic", "black", "mystery", "mysterious", "moon", "moonlight", "noir", "obscure", "void", "raven", "gloom", "gloomy", "macabre", "death", "melancholy", "somber", "sinister", "haunting", "eerie"),
        LumoraTheme.COQUETTE to setOf("love", "loving", "pink", "sweet", "sweetness", "coquette", "cute", "adorable", "affection", "darling", "charm", "charming", "flirt", "flirty", "blush", "blushing", "rose", "roses", "kiss", "kisses", "lace", "delicate", "tender", "gentle", "beauty", "beautiful", "pretty"),
        LumoraTheme.PIRATE to setOf("sea", "ocean", "pirate", "pirates", "adventure", "adventures", "ship", "ships", "treasure", "treasures", "sail", "sailing", "island", "islands", "rum", "captain", "voyage", "voyages", "map", "maps", "parrot", "anchor", "skull", "compass", "storm", "waves", "port", "harbor"),
        LumoraTheme.NATURE to setOf("nature", "natural", "tree", "trees", "green", "earth", "forest", "forests", "leaf", "leaves", "river", "rivers", "mountain", "mountains", "wild", "wilderness", "flower", "flowers", "meadow", "meadows", "breeze", "wind", "garden", "gardens", "root", "roots", "wood", "woods", "sun", "sunshine", "sky", "cloud", "clouds", "grass", "flora", "fauna"),
        LumoraTheme.ROMANCE to setOf("romance", "romantic", "rose", "roses", "heart", "hearts", "cupid", "valentine", "sweetheart", "passion", "passionate", "petal", "petals", "embrace", "embracing", "forever", "eternal", "desire", "longing", "beloved", "soulmate", "intimacy", "devotion", "adoration"),
        LumoraTheme.GRUNGE to setOf("grunge", "grungy", "dirty", "worn", "distressed", "punk", "rebel", "rebellion", "urban", "street", "graffiti", "angst", "angry", "static", "concrete", "rough", "raw", "broken", "torn", "faded", "weathered", "gritty"),
        LumoraTheme.MYSTIC to setOf("magic", "magical", "mystic", "mystical", "enchantment", "enchanted", "spiritual", "spirit", "aura", "cosmos", "cosmic", "galaxy", "celestial", "dream", "dreams", "fantasy", "esoteric", "oracle", "alchemy", "mysterious", "supernatural", "divine", "transcendent", "ethereal", "astral"),
        LumoraTheme.CYBER to setOf("cyber", "cybernetic", "tech", "technology", "digital", "code", "coding", "matrix", "neon", "circuit", "circuits", "glitch", "virtual", "hacker", "hackers", "binary", "future", "futuristic", "android", "androids", "synth", "synthetic", "artificial", "robot", "robots", "data", "network"),
        LumoraTheme.VAMPIRE to setOf("vampire", "vampires", "blood", "bloody", "red", "crimson", "nightmare", "nightmares", "fang", "fangs", "bat", "bats", "scarlet", "immortal", "immortality", "gothic", "bite", "biting", "grave", "graves", "moonlight", "eternal", "undead", "dark", "darkness", "bite", "neck", "thirst")
    )

    fun detect(text: String): LumoraTheme {
        // Whole-word matches avoid categorizing unrelated text by a substring.
        // A quote with no recognized words must retain the default theme.
        val words = Regex("[\\p{L}\\p{N}]+")
            .findAll(text.lowercase(Locale.ROOT))
            .map { it.value }
            .toSet()
        return themeKeywords.entries
            .map { (theme, keywords) -> theme to keywords.count { it in words } }
            .filter { (_, score) -> score > 0 }
            .maxByOrNull { (_, score) -> score }
            ?.first ?: LumoraTheme.DEFAULT
    }


}
