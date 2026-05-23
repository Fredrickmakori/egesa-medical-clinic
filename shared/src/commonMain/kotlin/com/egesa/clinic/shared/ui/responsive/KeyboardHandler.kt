package com.egesa.clinic.shared.ui.responsive

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*

/**
 * Keyboard handler for desktop PIN entry.
 * Captures numeric keys, backspace, and Enter for PIN submission.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun KeyboardAwarePinEntry(
    modifier: Modifier = Modifier,
    onDigitPressed: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.onKeyEvent { keyEvent ->
            when {
                // Numeric keys 0-9
                keyEvent.key.keyCode in 7..16 && keyEvent.type == KeyEventType.KeyDown -> {
                    val digit = (keyEvent.key.keyCode - 7).toString()
                    onDigitPressed(digit)
                    true
                }
                // Phone numeric keys (alternative layout)
                keyEvent.key.keyCode in 145..154 && keyEvent.type == KeyEventType.KeyDown -> {
                    val digit = (keyEvent.key.keyCode - 145).toString()
                    onDigitPressed(digit)
                    true
                }
                // Backspace
                keyEvent.key == Key.Backspace && keyEvent.type == KeyEventType.KeyDown -> {
                    onBackspace()
                    true
                }
                // Enter to submit
                keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown -> {
                    onSubmit()
                    true
                }
                // Allow Escape to do nothing (prevent default behavior)
                keyEvent.key == Key.Escape -> true
                else -> false
            }
        }
    ) {
        content()
    }
}

/**
 * Tracks keyboard state for PIN entry validation
 */
class KeyboardState {
    var lastKeyEvent: KeyEvent? = null
    var inputMethodAutoDetected: Boolean = false
}

enum class KeyboardInputType {
    NUMERIC,
    FUNCTIONAL,
    NONE
}

fun KeyEvent.getInputType(): KeyboardInputType = when {
    this.key.keyCode in 7..16 || this.key.keyCode in 145..154 -> KeyboardInputType.NUMERIC
    this.key == Key.Backspace || this.key == Key.Enter -> KeyboardInputType.FUNCTIONAL
    else -> KeyboardInputType.NONE
}

