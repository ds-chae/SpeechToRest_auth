package com.example.speechtorest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
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
                var status by remember { mutableStateOf("대기 중") }
                var isListening by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                DisposableEffect(Unit) {
                    stt = SpeechToText(
                        activity = this@MainActivity,
                        onPartial = { text -> transcript = text },
                        onFinal = { text -> transcript = text; status = "완료됨"; isListening = false },
                        onError = { err -> status = err; isListening = false }
                    )
                    onDispose { stt.destroy() }
                }

                Scaffold(
                    topBar = { TopAppBar(title = { Text("음성 → 텍스트 → REST") }) }
                ) { padding ->
                    Column(
                        Modifier
                            .padding(padding)
                            .padding(16.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("상태: $status")
                        OutlinedTextField(
                            value = transcript,
                            onValueChange = { transcript = it },
                            label = { Text("인식된 텍스트") },
                            trailingIcon = {
                                if (transcript.isNotEmpty()) {
                                    IconButton(onClick = { transcript = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "텍스트 지우기"
                                        )
                                    }
                                }
                            },
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
                                        status = "듣는 중…"
                                        isListening = true
                                        stt.start(langTag = "ko-KR")
                                    }
                                } else {
                                    // Stop
                                    stt.stop()
                                    status = "중지됨"
                                    isListening = false
                                }
                            }) {
                                Text(if (!isListening) "녹음 시작" else "녹음 중지")
                            }

                            Button(onClick = {
                                scope.launch {
                                    status = "전송 중…"
                                    try {
                                        val res = ApiProvider.api.sendText(SpeechPayload(transcript))
                                        status = "서버: ${res.status} (id=${res.id ?: "-"})"
                                    } catch (e: Exception) {
                                        status = "네트워크 오류: ${e.message}"
                                    }
                                }
                            }, enabled = transcript.isNotBlank()) {
                                Text("서버로 전송")
                            }
                        }
                    }
                }
            }
        }
    }
}
