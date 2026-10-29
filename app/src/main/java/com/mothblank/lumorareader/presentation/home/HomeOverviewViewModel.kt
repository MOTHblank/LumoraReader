package com.mothblank.lumorareader.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mothblank.lumorareader.data.ReadingActivityRepository
import com.mothblank.lumorareader.data.RecentBooksRepository
import com.mothblank.lumorareader.domain.model.ReadingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class HomeOverview(
    val bookCount: Int = 0,
    val currentlyReading: Int = 0,
    val foregroundMillisWeek: Long = 0L,
    val sessionCountWeek: Int = 0
)

@HiltViewModel
class HomeOverviewViewModel @Inject constructor(
    books: RecentBooksRepository,
    activity: ReadingActivityRepository
) : ViewModel() {
    private val since = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    val overview: StateFlow<HomeOverview> = combine(
        books.observeRecentBooks(),
        activity.observeTotals(since)
    ) { library, totals ->
        HomeOverview(
            bookCount = library.size,
            currentlyReading = library.count { it.readingStatus == ReadingStatus.READING },
            foregroundMillisWeek = totals.recentMillis,
            sessionCountWeek = totals.recentSessions
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), HomeOverview())
}
