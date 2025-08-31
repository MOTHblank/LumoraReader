package com.mothblank.lumorareader.features.widget

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState

class RefreshActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Clear the state to show loading indicator
        updateAppWidgetState(context, glanceId) {
            it.remove(stringPreferencesKey("quote_text"))
            it.remove(stringPreferencesKey("quote_author"))
        }
        // Force the widget to update
        QuoteWidget().update(context, glanceId)
    }
}
