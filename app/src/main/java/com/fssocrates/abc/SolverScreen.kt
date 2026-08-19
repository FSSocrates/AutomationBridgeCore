package com.fssocrates.abc

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolverScreen(onVerificationComplete: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Complete Verification") })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                factory = { context ->
                    val wv = ABCWebViewHolder.getOrCreate(context)
                    // Ensure parent is cleared
                    (wv.parent as? ViewGroup)?.removeView(wv)
                    ABCWebViewHolder.markAttachedToUi(true)
                    wv
                },
                modifier = Modifier.fillMaxSize(),
                update = { webView ->
                    // Keep alive; page already loaded by service
                }
            )
        }
    }

    // Simple auto-finish heuristic: listen for needsVerification going false
    // In production, call onVerificationComplete from JS bridge or user button.
    // For now, expose a way via ABC or manual finish.
    DisposableEffect(Unit) {
        onDispose {
            ABCWebViewHolder.markAttachedToUi(false)
        }
    }
}
