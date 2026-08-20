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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private var statusText by mutableStateOf("State: IDLE")
    private var resultText by mutableStateOf("Result: —")
    private var urlText by mutableStateOf("https://example.com")
    private var scriptText by mutableStateOf("ABC.result(location.href); ABC.complete();")

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            when (i?.action) {
                IpcProtocol.BROADCAST_RESULT -> {
                    resultText = "Result: ${i.getStringExtra(IpcProtocol.EXTRA_RESULT_URL)}"
                    statusText = "State: RESULT (${i.getStringExtra(IpcProtocol.EXTRA_JOB_ID)})"
                }
                IpcProtocol.BROADCAST_EVENT -> {
                    statusText = "State: ${i.getStringExtra(IpcProtocol.EXTRA_EVENT)} (${i.getStringExtra(IpcProtocol.EXTRA_JOB_ID)})"
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

        val Mod = androidx.compose.ui.Modifier
        setContent {
            MaterialTheme {
                Column(
                    modifier = Mod.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ABC Demo", style = MaterialTheme.typography.headlineMedium)
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        label = { Text("URL") },
                        modifier = Mod.fillMaxWidth().padding(top = 16.dp)
                    )
                    OutlinedTextField(
                        value = scriptText,
                        onValueChange = { scriptText = it },
                        label = { Text("Script") },
                        modifier = Mod.fillMaxWidth().padding(top = 8.dp),
                        minLines = 3
                    )
                    Text(statusText, modifier = Mod.padding(top = 16.dp))
                    Text(resultText, modifier = Mod.padding(top = 4.dp))
                    Button(
                        onClick = {
                            statusText = "State: STARTING"
                            AutomationBridge.start(this@MainActivity, urlText, scriptText)
                        },
                        modifier = Mod.padding(top = 16.dp)
                    ) { Text("Start job") }
                    Button(onClick = { AutomationBridge.status(this@MainActivity) }, modifier = Mod.padding(top = 8.dp)) {
                        Text("Status")
                    }
                    Button(onClick = { AutomationBridge.resume(this@MainActivity) }, modifier = Mod.padding(top = 8.dp)) {
                        Text("Resume")
                    }
                    Button(
                        onClick = {
                            AutomationBridge.cancel(this@MainActivity)
                            statusText = "State: CANCELLED"
                        },
                        modifier = Mod.padding(top = 8.dp)
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
