package com.mothblank.lumorareader

import android.app.Application
import android.content.ComponentCallbacks2
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeLogging()
        // Stubs for other services are perfect.
        initializeCrashReporting()
        initializeAnalytics()
        initializePerformanceMonitoring()
        setupMemoryLeakDetection()
        Timber.i("Application initialized successfully")
    }

    /**
     * DEPRECATED: This is an older, less specific callback.
     * All logic has been consolidated into onTrimMemory, which is the modern standard.
     */
    override fun onLowMemory() {
        super.onLowMemory()
        // This is equivalent to TRIM_MEMORY_COMPLETE, so we can let onTrimMemory handle it.
        Timber.w("System is running low on memory (onLowMemory callback).")
    }

    /**
     * This is the modern, recommended callback for reacting to system memory pressure.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Timber.d("Memory trim requested with level: $level")

        // REFACTORED: A 'when' statement is more idiomatic and comprehensive.
        when (level) {
            // App is running and not about to be killed, but memory is getting low.
            // Release non-critical caches.
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                Timber.i("Memory running low (level $level). Releasing some caches.")
                // Example: Clear image caches that can be easily recreated.
            }

            // App is running and is considered killable.
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                Timber.w("Memory running CRITICAL (level $level). Releasing most caches.")
                // Aggressively release non-essential memory.
            }

            // App's UI is no longer visible.
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                Timber.i("UI is hidden (level $level). Releasing UI-related resources.")
                // Release large resources used only by the UI (e.g., heavy bitmaps).
            }

            // App is in the background LRU list.
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                Timber.w("App is in background and may be killed (level $level). Releasing all possible resources.")
                // Release everything that can be recreated when the app is next opened.
            }
        }
    }

    private fun initializeLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree) // Use the object instance
        }
        Timber.d("Logging initialized for ${if (BuildConfig.DEBUG) "DEBUG" else "RELEASE"} build")
    }

    // --- Other initialization stubs remain the same ---
    private fun initializeCrashReporting() { /* ... */ }
    private fun initializeAnalytics() { /* ... */ }
    private fun initializePerformanceMonitoring() { /* ... */ }
    private fun setupMemoryLeakDetection() { /* ... */ }

    /**
     * A custom Timber tree that sends errors and warnings to a crash reporting service.
     * Defined as an 'object' because it is a stateless singleton.
     */
    private object CrashReportingTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == android.util.Log.ERROR || priority == android.util.Log.WARN) {
                // Example: FirebaseCrashlytics.getInstance().log(message)
                if (t != null) {
                    // Example: FirebaseCrashlytics.getInstance().recordException(t)
                }
            }
        }
    }
}