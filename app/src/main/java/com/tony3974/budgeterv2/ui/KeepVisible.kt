package com.tony3974.budgeterv2.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Scrolls a text field back into view once the keyboard is up. With edge-to-edge the window no
 * longer shrinks for the keyboard, so the scrolling column pads for it (imePadding) and each field
 * asks to be brought into view while it has focus.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun Modifier.keepVisibleWhenFocused(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val margin = with(LocalDensity.current) { VisibleMargin.toPx() }
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(focused, imeVisible) {
        if (focused && imeVisible) {
            delay(250)
            // Ask for a bit more than the field itself, so it doesn't end up flush against the
            // top of the keyboard.
            requester.bringIntoView(
                Rect(0f, -margin, size.width.toFloat(), size.height + margin),
            )
        }
    }
    return this
        .onSizeChanged { size = it }
        .bringIntoViewRequester(requester)
        .onFocusEvent { focused = it.isFocused }
}

/** Space kept between a focused field and the keyboard (or the top of the screen). */
private val VisibleMargin = 16.dp
