package com.fssocrates.abc

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier.modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun SolverScreen(onVerificationComplete: () -> Unit = {}) {
    AndroidView(
        factory = { context ->
            val wv = ABCWebViewHolder.getOrCreate(context)
            (wv.parent as? ViewGroup)?.removeView(wv)
            ABCWebViewHolder.markAttachedToUi(true)
            wv
        },
        modifier = Modifier.fillMaxSize()
    )
    DisposableEffect(Unit) {
        onDispose { ABCWebViewHolder.markAttachedToUi(false) }
    }
}
