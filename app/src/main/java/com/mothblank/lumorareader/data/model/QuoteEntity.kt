package com.mothblank.lumorareader.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mothblank.lumorareader.domain.model.Quote

@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val text: String,
    val author: String,
    val theme: String?
)

// Função de extensão para mapear a entidade para o modelo de domínio
fun QuoteEntity.toDomain(): Quote {
    return Quote(id = id, text = text, author = author, theme = theme)
}

// Função de extensão para mapear o modelo de domínio para a entidade
fun Quote.toEntity(): QuoteEntity {
    return QuoteEntity(id = id, text = text, author = author, theme = theme)
} 