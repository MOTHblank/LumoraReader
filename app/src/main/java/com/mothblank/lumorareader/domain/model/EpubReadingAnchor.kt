package com.mothblank.lumorareader.domain.model

/**
 * Stable EPUB location within an original spine document, irrespective of page layout.
 * A spineIndex of -1 represents a fractional fallback (e.g. image-only documents).
 */
data class EpubReadingAnchor(
    val spineIndex: Int,
    val characterOffset: Int,
    val fraction: Float
) {
    fun serialize(): String = "$spineIndex:$characterOffset:$fraction"

    companion object {
        fun create(spineIndex: Int, characterOffset: Int, fraction: Float): EpubReadingAnchor? {
            if (spineIndex < -1 || characterOffset < 0 || !fraction.isFinite()) return null
            return EpubReadingAnchor(spineIndex, characterOffset, fraction.coerceIn(0f, 1f))
        }

        fun parse(serialized: String?): EpubReadingAnchor? {
            val parts = serialized?.split(':') ?: return null
            if (parts.size != 3) return null
            return create(
                parts[0].toIntOrNull() ?: return null,
                parts[1].toIntOrNull() ?: return null,
                parts[2].toFloatOrNull() ?: return null
            )
        }
    }
}
