package com.mothblank.lumorareader.presentation.reader

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mothblank.lumorareader.R
import com.mothblank.lumorareader.domain.model.Page
import com.mothblank.lumorareader.data.EpubChapter
import com.mothblank.lumorareader.presentation.home.components.InteractiveBubble
import com.mothblank.lumorareader.ui.theme.LumoraTheme
import com.mothblank.lumorareader.ui.theme.representativeColor
import kotlin.math.roundToInt


@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun EpubScrollReader(viewModel: ReaderViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fileUri = uiState.epubFileUri
    val jsBundle = remember { generateJsBundle() }

    if (fileUri != null) {
        // Recreate the AndroidView when a generated HTML URL changes. Updating
        // the old WebView's selection flag alone cannot display a new section.
        key(fileUri) {
            var webView by remember { mutableStateOf<WebView?>(null) }
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            val activeView = this
                            settings.javaScriptEnabled = true
                            settings.allowContentAccess = false
                            settings.allowFileAccess = true
                            @Suppress("DEPRECATION")
                            settings.allowFileAccessFromFileURLs = false
                            @Suppress("DEPRECATION")
                            settings.allowUniversalAccessFromFileURLs = false
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                settings.safeBrowsingEnabled = true
                            }
                            settings.domStorageEnabled = true
                            isVerticalScrollBarEnabled = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.setSupportZoom(true)

                            addJavascriptInterface(object : Any() {
                                @JavascriptInterface
                                fun saveEpubPosition(pixelOffset: Int, spineIndex: Int, characterOffset: Int, fraction: Float) {
                                    activeView.post {
                                        viewModel.saveEpubPosition(fileUri, pixelOffset, spineIndex, characterOffset, fraction)
                                    }
                                }
                                @JavascriptInterface
                                fun onScrollNearBottom() {
                                    activeView.post {
                                        if (viewModel.uiState.value.epubFileUri == fileUri) {
                                            viewModel.onScrollNearEndOfSection()
                                        }
                                    }
                                }
                                @JavascriptInterface
                                fun onContentLoaded() = Unit
                                @JavascriptInterface
                                fun onTap() {
                                    activeView.post {
                                        if (viewModel.uiState.value.epubFileUri == fileUri) {
                                            viewModel.onToggleMenuVisibility()
                                        }
                                    }
                                }
                            }, "AndroidInterface")

                            addJavascriptInterface(object : Any() {
                                @JavascriptInterface
                                fun onTextSelected(text: String, spineIndex: Int, characterOffset: Int, fraction: Float) {
                                    activeView.post {
                                        if (viewModel.uiState.value.epubFileUri == fileUri) {
                                            viewModel.onTextSelected(
                                                text, fileUri, spineIndex, characterOffset, fraction
                                            )
                                        }
                                    }
                                }
                                @JavascriptInterface
                                fun onSelectionCleared() {
                                    activeView.post {
                                        if (viewModel.uiState.value.epubFileUri == fileUri) {
                                            viewModel.clearTextSelection()
                                        }
                                    }
                                }
                            }, "AndroidSelectionInterface")

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(v: WebView?, u: String?) {
                                    if (v == null || u != fileUri) return
                                    v.evaluateJavascript(jsBundle) {
                                        val anchor = viewModel.getEpubAnchor()
                                        val command = "window.lumoraRestorePosition(" +
                                            (anchor?.spineIndex ?: -1) + "," +
                                            (anchor?.characterOffset ?: 0) + "," +
                                            (anchor?.fraction ?: -1f) + "," +
                                            viewModel.getScrollPosition() + ");"
                                        v.evaluateJavascript(command, null)
                                        val passage = viewModel.uiState.value.quoteHighlightText
                                        if (!passage.isNullOrBlank() && anchor != null && anchor.spineIndex >= 0) {
                                            v.evaluateJavascript(
                                                "window.lumoraHighlightPassage?.(" +
                                                    anchor.spineIndex + "," +
                                                    anchor.characterOffset + "," +
                                                    JSONObject.quote(passage) + ");",
                                                null
                                            )
                                        }
                                    }
                                }
                            }
                            webView = this
                            loadUrl(fileUri)
                        }
                    },
                    update = { wv ->
                        wv.evaluateJavascript(
                            "window.isTextSelectionActive = " + uiState.isTextSelectionActive + ";", null
                        )
                        if (uiState.quoteHighlightText == null) {
                            wv.evaluateJavascript("window.lumoraClearQuoteHighlight?.();", null)
                        }
                    }
                )
            }
            DisposableEffect(fileUri) {
                viewModel.onEpubWebViewAttached(fileUri)
                onDispose {
                    webView?.let { w ->
                        w.stopLoading()
                        w.loadUrl("about:blank")
                        w.removeAllViews()
                        w.destroy()
                    }
                    webView = null
                    viewModel.onEpubWebViewReleased(fileUri)
                }
            }
        }
    } else if (uiState.isLoading) {
        LoadingView()
    }
}

// MARK: - JavaScript Bundle (Memoized)
private fun generateJsBundle(): String = """
    (function(){
        const sel = () => window.getSelection();
        const fire = (e, p) => window.AndroidInterface?.[e]?.(p);
        const LONG = 450, MAX_MOVE = 20, TAP_MOVE = 24, TAP_TIME = 280;
        let sx, sy, st;
        let longTimer;
        let scrollTimeout;
        
        // ✅ Add tap detection variables
        let tapStartX, tapStartY;
        let tapTimeout;
        
        // Capture a spine-specific text offset and scroll fraction. These
        // remain meaningful after changes to font size and viewport geometry.
        let restoring = false;
        const fractionOfScroll = () => {
            const max = Math.max(0, document.documentElement.scrollHeight - window.innerHeight);
            return max ? Math.min(1, Math.max(0, window.scrollY / max)) : 0;
        };
        const visibleTextAnchor = () => {
            const x = Math.max(1, Math.min(window.innerWidth - 1, window.innerWidth * 0.4));
            for (const y of [32, 64, window.innerHeight * 0.22, window.innerHeight * 0.45]) {
                const caret = document.caretRangeFromPoint?.(x, Math.min(y, window.innerHeight - 1));
                const node = caret?.startContainer;
                if (!node) continue;
                const element = node.nodeType === Node.ELEMENT_NODE ? node : node.parentElement;
                const section = element?.closest?.('[data-lumora-spine]');
                if (!section) continue;
                const range = document.createRange();
                range.selectNodeContents(section);
                try {
                    range.setEnd(node, caret.startOffset);
                    return {
                        spine: Number(section.dataset.lumoraSpine),
                        offset: range.toString().length
                    };
                } catch (_) { /* Fraction-only fallback for non-text content. */ }
            }
            return null;
        };
        const saveScroll = () => {
            if (restoring) return;
            clearTimeout(scrollTimeout);
            scrollTimeout = setTimeout(() => {
                if (restoring) return;
                const y = Math.round(window.pageYOffset || document.documentElement.scrollTop);
                const h = document.documentElement.scrollHeight;
                const v = window.innerHeight;
                if (!sel() || sel().isCollapsed) {
                    const anchor = visibleTextAnchor();
                    window.AndroidInterface?.saveEpubPosition?.(
                        y, anchor?.spine ?? -1, anchor?.offset ?? 0, fractionOfScroll()
                    );
                }
                if (y + v + 50 >= h) fire('onScrollNearBottom');
            }, 150);
        };
        window.lumoraRestorePosition = (spine, offset, fraction, oldPixels) => {
            restoring = true;
            const restore = () => {
                let restored = false;
                if (spine >= 0 && offset >= 0) {
                    const section = document.querySelector('[data-lumora-spine="' + spine + '"]');
                    if (section) {
                        const walker = document.createTreeWalker(section, NodeFilter.SHOW_TEXT);
                        let remaining = offset, node;
                        while ((node = walker.nextNode())) {
                            const len = node.textContent.length;
                            if (remaining <= len) {
                                const range = document.createRange();
                                range.setStart(node, Math.max(0, remaining));
                                range.setEnd(node, Math.min(len, remaining + 1));
                                const rect = range.getBoundingClientRect();
                                if (rect && rect.height > 0 && Number.isFinite(rect.top)) {
                                    window.scrollTo(0, Math.max(0, window.scrollY + rect.top - 28));
                                    restored = true;
                                }
                                break;
                            }
                            remaining -= len;
                        }
                    }
                }
                if (!restored && fraction >= 0 && fraction <= 1) {
                    const max = Math.max(0, document.documentElement.scrollHeight - window.innerHeight);
                    window.scrollTo(0, fraction * max);
                    restored = true;
                }
                if (!restored) window.scrollTo(0, Math.max(0, oldPixels));
            };
            requestAnimationFrame(() => {
                restore();
                // Images and fonts can settle after the first layout pass.
                setTimeout(restore, 220);
                setTimeout(() => {
                    restore();
                    restoring = false;
                }, 550);
            });
        };
        window.addEventListener('scroll', saveScroll, { passive: true });

        // Highlight the single passage reached from the quotation library.
        // DOM Range avoids rewriting book markup, links, and selection handlers.
        let quoteOverlay = null;
        let highlightedRange = null;
        const updateQuoteOverlay = () => {
            if (!quoteOverlay || !highlightedRange) return;
            quoteOverlay.replaceChildren();
            Array.from(highlightedRange.getClientRects()).slice(0, 80).forEach(rect => {
                if (rect.width < 1 || rect.height < 1) return;
                const mark = document.createElement('div');
                mark.style.cssText = 'position:absolute;background:rgba(250,184,57,.38);border-radius:3px;';
                mark.style.left = rect.left + 'px';
                mark.style.top = rect.top + 'px';
                mark.style.width = rect.width + 'px';
                mark.style.height = rect.height + 'px';
                quoteOverlay.appendChild(mark);
            });
        };
        window.lumoraClearQuoteHighlight = () => {
            if (window.CSS?.highlights) CSS.highlights.delete('lumora-return-passage');
            quoteOverlay?.remove();
            quoteOverlay = null;
            highlightedRange = null;
        };
        window.lumoraHighlightPassage = (spine, offset, text) => {
            window.lumoraClearQuoteHighlight();
            if (!text || text.length > 2000 || offset < 0) return;
            const section = document.querySelector('[data-lumora-spine="' + spine + '"]');
            if (!section) return;
            const walker = document.createTreeWalker(section, NodeFilter.SHOW_TEXT);
            let node, consumed = 0, startNode = null, endNode = null, startOffset = 0, endOffset = 0;
            const end = offset + text.length;
            while ((node = walker.nextNode())) {
                const length = node.textContent.length;
                if (!startNode && offset < consumed + length) {
                    startNode = node;
                    startOffset = offset - consumed;
                }
                if (startNode && end <= consumed + length) {
                    endNode = node;
                    endOffset = Math.max(0, end - consumed);
                    break;
                }
                consumed += length;
            }
            if (!startNode || !endNode) return;
            const range = document.createRange();
            try {
                range.setStart(startNode, startOffset);
                range.setEnd(endNode, endOffset);
            } catch (_) { return; }
            const normalize = s => s.replace(/\s+/g, ' ').trim();
            if (!normalize(range.toString()).startsWith(normalize(text).slice(0, 24))) return;
            if (window.CSS?.highlights && window.Highlight) {
                CSS.highlights.set('lumora-return-passage', new Highlight(range));
            } else {
                highlightedRange = range;
                quoteOverlay = document.createElement('div');
                quoteOverlay.style.cssText =
                    'position:fixed;inset:0;pointer-events:none;z-index:2147483646;';
                document.body.appendChild(quoteOverlay);
                updateQuoteOverlay();
            }
        };
        let highlightFrame = false;
        const refreshHighlight = () => {
            if (!quoteOverlay || highlightFrame) return;
            highlightFrame = true;
            requestAnimationFrame(() => {
                highlightFrame = false;
                updateQuoteOverlay();
            });
        };
        window.addEventListener('scroll', refreshHighlight, { passive: true });
        window.addEventListener('resize', refreshHighlight);

        document.addEventListener('touchstart', (e) => {
            if (e.touches.length === 0) return;
            const t = e.touches[0]; sx = t.clientX; sy = t.clientY; st = Date.now();
            
            // ✅ Add tap detection initialization
            tapStartX = t.clientX;
            tapStartY = t.clientY;
            clearTimeout(tapTimeout);
            tapTimeout = setTimeout(() => {
                tapStartX = tapStartY = null;
            }, 500);
            
            if (!sel()?.isCollapsed) return;
            longTimer = setTimeout(() => {
                const dx = Math.abs(e.touches[0].clientX - sx);
                const dy = Math.abs(e.touches[0].clientY - sy);
                if (dx > MAX_MOVE || dy > MAX_MOVE) return;
                const r = document.caretRangeFromPoint(sx, sy);
                if (!r || r.startContainer.nodeType !== Node.TEXT_NODE) return;
                const txt = r.startContainer.textContent;
                let s = r.startOffset, e = r.startOffset;
                while (s > 0 && /\w/.test(txt[s - 1])) s--;
                while (e < txt.length && /\w/.test(txt[e])) e++;
                r.setStart(r.startContainer, s);
                r.setEnd(r.startContainer, e);
                sel()?.removeAllRanges();
                sel()?.addRange(r);
            }, LONG);
        }, { passive: true });
        
        // ✅ Add tap detection
        document.addEventListener('touchend', (e) => {
            clearTimeout(longTimer);
            
            // ✅ Add tap detection logic
            if (!tapStartX || !tapStartY) return;
            const touch = e.changedTouches[0];
            const dx = Math.abs(touch.clientX - tapStartX);
            const dy = Math.abs(touch.clientY - tapStartY);
            if (dx < 20 && dy < 20) {
                window.AndroidInterface?.onTap?.();
            }
        });
        
        document.addEventListener('touchmove', () => clearTimeout(longTimer));
        document.addEventListener('selectionchange', () => {
            const selection = sel();
            const txt = selection?.toString()?.trim();
            if (txt && txt.length > 3 && selection.rangeCount > 0) {
                const range = selection.getRangeAt(0);
                const node = range.startContainer;
                const element = node.nodeType === Node.ELEMENT_NODE ? node : node.parentElement;
                const section = element?.closest?.('[data-lumora-spine]');
                let spine = -1, offset = 0;
                if (section) {
                    try {
                        const preceding = document.createRange();
                        preceding.selectNodeContents(section);
                        preceding.setEnd(node, range.startOffset);
                        spine = Number(section.dataset.lumoraSpine);
                        offset = preceding.toString().length;
                    } catch (_) { /* Keep section fraction when markup is unusual. */ }
                }
                window.AndroidSelectionInterface?.onTextSelected(
                    txt, spine, offset, fractionOfScroll()
                );
            } else {
                window.AndroidSelectionInterface?.onSelectionCleared();
            }
        });
        window.AndroidInterface?.onContentLoaded();
    })();
""".trimIndent()

// MARK: - JS Interface for Scroll & Content
class WebViewScrollInterface(private val viewModel: ReaderViewModel) {
    @JavascriptInterface
    fun saveScrollPosition(pos: Int) {
        viewModel.saveScrollPosition(pos)
    }

    @JavascriptInterface
    fun onScrollNearBottom() {
        viewModel.onScrollNearEndOfSection()
    }

    @JavascriptInterface
    fun onContentLoaded() {
        // Optional
    }
}

// MARK: - JS Interface for Text Selection
class WebViewSelectionInterface(private val viewModel: ReaderViewModel) {
    @JavascriptInterface
    fun onTextSelected(text: String) {
        viewModel.onTextSelected(text)
    }

    @JavascriptInterface
    fun onSelectionCleared() {
        viewModel.clearTextSelection()
    }
}

