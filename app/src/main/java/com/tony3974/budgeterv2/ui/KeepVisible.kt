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
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(focused, imeVisible) {
        if (focused && imeVisible) {
            delay(250)
            requester.bringIntoView()
        }
    }
    return this.bringIntoViewRequester(requester).onFocusEvent { focused = it.isFocused }
}
