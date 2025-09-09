package com.example.speechtorest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var stt: SpeechToText

    private val requestMic = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // No-op, UI reacts to state
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var transcript by remember { mutableStateOf("") }
                var status by remember { mutableStateOf("Idle") }
                var isListening by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                DisposableEffect(Unit) {
                    stt = SpeechToText(
                        activity = this@MainActivity,
                        onPartial = { text -> transcript = text },
                        onFinal = { text -> transcript = text; status = "Finalized"; isListening = false },
                        onError = { err -> status = err; isListening = false }
                    )
                    onDispose { stt.destroy() }
                }

                Scaffold(
                    topBar = { TopAppBar(title = { Text("Speech → Text → REST") }) }
                ) { padding ->
                    Column(
                        Modifier
                            .padding(padding)
                            .padding(16.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Status: $status")
                        OutlinedTextField(
                            value = transcript,
                            onValueChange = { transcript = it },
                            label = { Text("Transcribed text") },
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = {
                                if (!isListening) {
                                    // Start
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!hasPerm) {
                                        requestMic.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        status = "Listening…"
                                        isListening = true
                                        stt.start(langTag = "en-US")
                                    }
                                } else {
                                    // Stop
                                    stt.stop()
                                    status = "Stopped"
                                    isListening = false
                                }
                            }) {
                                Text(if (!isListening) "Start Recording" else "Stop Recording")
                            }

                            Button(onClick = {
                                scope.launch {
                                    status = "Sending…"
                                    try {
                                        val res = ApiProvider.api.sendText(SpeechPayload(transcript))
                                        status = "Server: ${'$'}{res.status} (id=${'$'}{res.id ?: "-"})"
                                    } catch (e: Exception) {
                                        status = "Network error: ${'$'}{e.message}"
                                    }
                                }
                            }, enabled = transcript.isNotBlank()) {
                                Text("Send to Server")
                            }
                        }
                    }
                }
            }
        }
    }
}
