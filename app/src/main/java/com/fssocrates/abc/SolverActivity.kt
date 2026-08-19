package com.fssocrates.abc

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier.Modifier
import androidx.compose.ui.unit.dp

class SolverActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val jobId = intent.getStringExtra(ABCForegroundService.EXTRA_JOB_ID) ?: ""
        val reason = intent.getStringExtra(ABCForegroundService.EXTRA_REASON) ?: "verification"

        setContent {
            MaterialTheme {
                Scaffold(
                    topBar = { TopAppBar(title = { Text("Action required: $reason") }) }
                ) { padding ->
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        SolverScreen(onVerificationComplete = { /* explicit only via button */ })
                        Button(
                            onClick = { completeInteraction() },
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("Done — Resume automation")
                        }
                    }
                }
            }
        }
    }

    private fun completeInteraction() {
        // Explicit user confirmation — do not rely on onDestroy
        ABCWebViewHolder.markAttachedToUi(false)
        ABCWebViewHolder.setNeedsVerification(false)
        ABCNotificationManager.cancelHigh(this)
        // Tell service to resume
        startService(Intent(this, ABCForegroundService::class.java).apply {
            // Service can expose resume via sticky or we use a static / binder later
        })
        // Direct engine resume via shared ABC
        ABC.engine?.resumeAfterUserInteraction()
        finish()
    }

    override fun onDestroy() {
        // Do NOT treat destroy as success
        ABCWebViewHolder.markAttachedToUi(false)
        super.onDestroy()
    }
}
