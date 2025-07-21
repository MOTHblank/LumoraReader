package com.mothblank.lumorareader.presentation.quotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mothblank.lumorareader.domain.model.Quote
import com.mothblank.lumorareader.domain.repository.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuoteViewModel @Inject constructor(
    private val repository: QuoteRepository
) : ViewModel() {

    val quotes = repository.getAllQuotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addQuote(text: String, author: String) {
        viewModelScope.launch {
            if (text.isNotBlank() && author.isNotBlank()) {
                repository.saveQuote(Quote(text = text, author = author))
            }
        }
    }
} 