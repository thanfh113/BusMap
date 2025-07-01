package com.example.busmap

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

@Composable
fun DismissKeyboard(content: @Composable () -> Unit) {
    val focusManager = LocalFocusManager.current
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()//.clickable
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        content()
    }
}

