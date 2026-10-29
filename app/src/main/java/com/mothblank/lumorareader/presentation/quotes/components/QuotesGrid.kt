package com.mothblank.lumorareader.presentation.quotes.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mothblank.lumorareader.domain.model.Quote

@Composable
fun QuotesGrid(
    quotes: List<Quote>,
    gridState: LazyStaggeredGridState,
    onDeleteClicked: (Quote) -> Unit,
    onEditClicked: (Quote) -> Unit,
    onAddToWidgetClick: (Quote) -> Unit,
    onJumpClick: (Quote) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
        modifier = modifier.fillMaxSize(),
        state = gridState,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp
    ) {
        items(quotes, key = { it.id }) { quote ->
            EnhancedQuoteCard(
                quote = quote,
                onEditClick = onEditClicked,
                onAddToWidgetClick = onAddToWidgetClick,
                onDeleteClick = onDeleteClicked,
                onJumpClick = onJumpClick
            )
        }
    }
}