package com.fssocrates.abc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
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

class ManualInteractionActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reason = intent.getStringExtra(ABCForegroundService.EXTRA_REASON) ?: "verification"

        setContent {
            MaterialTheme {
                Scaffold(
                    topBar = { TopAppBar(title = { Text("Action required: $reason") }) }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        SolverScreen()
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
        ABCWebViewHolder.markAttachedToUi(false)
        ABCWebViewHolder.setNeedsVerification(false)
        ABCNotificationManager.cancelHigh(this)
        ABC.engine?.resumeAfterUserInteraction()
        finish()
    }

    override fun onDestroy() {
        ABCWebViewHolder.markAttachedToUi(false)
        super.onDestroy()
    }
}
