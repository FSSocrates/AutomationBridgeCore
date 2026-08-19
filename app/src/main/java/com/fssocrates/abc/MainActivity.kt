package com.fssocrates.abc

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
import androidx.compose.ui.Modifier.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("AutomationBridgeCore", style = MaterialTheme.typography.headlineMedium)
                    Text("Demo host for the ABC engine", modifier = Modifier.padding(vertical = 16.dp))
                    Button(onClick = {
                        AutomationBridge.start(
                            this@MainActivity,
                            url = "https://example.com",
                            script = "ABC.result(window.location.href);"
                        )
                    }) { Text("Start demo job") }
                    Button(
                        onClick = { AutomationBridge.cancel(this@MainActivity) },
                        modifier = Modifier.padding(top = 8.dp)
                    ) { Text("Cancel") }
                }
            }
        }
    }
}
