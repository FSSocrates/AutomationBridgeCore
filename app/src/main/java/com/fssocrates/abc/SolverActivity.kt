package com.fssocrates.abc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

class SolverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SolverScreen(
                        onVerificationComplete = {
                            // Detach and resume background
                            ABCWebViewHolder.markAttachedToUi(false)
                            ABCWebViewHolder.setNeedsVerification(false)
                            // Notify service to downgrade notification
                            val svc = Intent(this, ABCForegroundService::class.java)
                            // Service will handle via shared state; force low notification
                            ABCNotificationManager.showLow(this)
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        ABCWebViewHolder.markAttachedToUi(false)
        super.onDestroy()
    }
}
