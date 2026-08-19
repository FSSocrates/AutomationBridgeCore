package com.fssocrates.abc

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("AutomationBridgeCore", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Background WebView engine for IPC automation.",
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    Button(onClick = {
                        val i = Intent(this@MainActivity, ABCForegroundService::class.java).apply {
                            putExtra(ABCForegroundService.EXTRA_TARGET_URL, "https://example.com")
                            putExtra(
                                ABCForegroundService.EXTRA_SCRIPT,
                                "ABC.sendResult(window.location.href);"
                            )
                        }
                        startForegroundService(i)
                    }) {
                        Text("Start Demo Engine")
                    }
                }
            }
        }
    }
}
