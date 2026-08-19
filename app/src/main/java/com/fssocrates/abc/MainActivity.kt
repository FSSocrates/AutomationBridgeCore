package com.fssocrates.abc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private var statusText by mutableStateOf("State: IDLE")
    private var resultText by mutableStateOf("Result: —")

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            when (i?.action) {
                IpcProtocol.BROADCAST_RESULT -> {
                    val url = i.getStringExtra(IpcProtocol.EXTRA_RESULT_URL)
                    val job = i.getStringExtra(IpcProtocol.EXTRA_JOB_ID)
                    resultText = "Result [$job]: $url"
                    statusText = "State: COMPLETED"
                }
                IpcProtocol.BROADCAST_EVENT -> {
                    val ev = i.getStringExtra(IpcProtocol.EXTRA_EVENT) ?: "?"
                    val job = i.getStringExtra(IpcProtocol.EXTRA_JOB_ID)
                    statusText = "State: $ev ($job)"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filter = IntentFilter().apply {
            addAction(IpcProtocol.BROADCAST_RESULT)
            addAction(IpcProtocol.BROADCAST_EVENT)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(receiver, filter)
        }

        setContent {
            MaterialTheme {
                Column(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ABC Sample Client", style = MaterialTheme.typography.headlineMedium)
                    Text(statusText, modifier = androidx.compose.ui.Modifier.padding(top = 16.dp))
                    Text(resultText, modifier = androidx.compose.ui.Modifier.padding(top = 8.dp))
                    Button(
                        onClick = {
                            statusText = "State: STARTING"
                            AutomationBridge.start(
                                this@MainActivity,
                                url = "https://example.com",
                                script = "ABC.result(window.location.href, \'URL\');"
                            )
                        },
                        modifier = androidx.compose.ui.Modifier.padding(top = 24.dp)
                    ) { Text("Start job") }
                    Button(
                        onClick = { AutomationBridge.status(this@MainActivity) },
                        modifier = androidx.compose.ui.Modifier.padding(top = 8.dp)
                    ) { Text("Status") }
                    Button(
                        onClick = {
                            AutomationBridge.cancel(this@MainActivity)
                            statusText = "State: CANCELLED"
                        },
                        modifier = androidx.compose.ui.modifier.Modifier.padding(top = 8.dp)
                    ) { Text("Cancel") }
                }
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }
}
