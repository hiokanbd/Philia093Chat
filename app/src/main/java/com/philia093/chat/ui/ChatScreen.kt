package com.philia093.chat.ui

import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.philia093.chat.network.ChatClient
import com.philia093.chat.settings.AppSettings
import kotlinx.coroutines.launch

private val PinkCherry = Color(0xFFFFB7C5)
private val PurpleLavender = Color(0xFFD8B4E2)
private val TextMauve = Color(0xFF5E4B66)
private val BgCream = Color(0xFFFFF8F0)
private val BubbleUser = Color(0xFFFFE4E8)
private val BubbleXilian = Color(0xFFF3E8F7)

data class Message(
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var input by remember { mutableStateOf("") }
    var connected by remember { mutableStateOf<Boolean?>(null) }
    val listState = rememberLazyListState()

    // Settings flows
    val bgUri by AppSettings.bgUriFlow(context).collectAsState(initial = "")
    val bgBrightness by AppSettings.bgBrightnessFlow(context).collectAsState(initial = AppSettings.DEFAULT_BG_BRIGHTNESS)
    val bubbleAlpha by AppSettings.bubbleAlphaFlow(context).collectAsState(initial = AppSettings.DEFAULT_BUBBLE_ALPHA)
    val apiKey by AppSettings.apiKeyFlow(context).collectAsState(initial = "")

    // Character name from backend
    var characterName by remember { mutableStateOf("昔涟") }
    var characterLetter by remember { mutableStateOf("涟") }

    // Voice input
    var listening by remember { mutableStateOf(false) }
    val recognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    // Check connection + load character
    LaunchedEffect(Unit) {
        connected = ChatClient.checkHealth()
        if (connected == true) {
            val chars = ChatClient.getCharacters()
            if (chars.active != null) {
                val active = chars.characters.find { it.name == chars.active }
                if (active != null) {
                    characterName = active.displayName
                    characterLetter = active.avatarLetter
                }
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // Voice recognition listener
    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    input = matches[0]
                }
                listening = false
            }
            override fun onError(error: Int) { listening = false }
            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false }
            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        }
        recognizer.setRecognitionListener(listener)
        onDispose { recognizer.destroy() }
    }

    fun sendMessage() {
        val msg = input.trim()
        if (msg.isEmpty()) return
        input = ""

        messages = messages + Message(msg, isUser = true)
        val loadingMsg = Message("${characterName}正在翻书…", isUser = false, isLoading = true)
        messages = messages + loadingMsg

        scope.launch {
            val result = ChatClient.send(msg, apiKey)
            messages = messages.filter { it != loadingMsg }
            messages = messages + Message(
                text = if (result.error != null) result.error else result.reply,
                isUser = false
            )
        }
    }

    // ── UI ──
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        if (bgUri.isNotEmpty()) {
            AsyncImage(
                model = bgUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Brightness overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 1f - bgBrightness))
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (connected) {
                                            true -> Color(0xFF4CAF50)
                                            false -> Color(0xFFFF5252)
                                            null -> Color(0xFFFFC107)
                                        }
                                    )
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(characterName, color = TextMauve, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Menu, "设置", tint = TextMauve)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White.copy(alpha = bubbleAlpha)
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Chat messages
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$characterName\n\n轻轻翻开书页，等你说第一句话…♪",
                                    color = TextMauve.copy(alpha = bubbleAlpha),
                                    fontSize = 14.sp,
                                    lineHeight = 24.sp
                                )
                            }
                        }
                    }

                    items(messages, key = { messages.indexOf(it) }) { msg ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically { it / 2 }
                        ) {
                            MessageBubble(msg, bubbleAlpha)
                        }
                    }
                }

                HorizontalDivider(color = PinkCherry.copy(alpha = 0.3f))

                // Input area
                Surface(
                    color = Color.White.copy(alpha = bubbleAlpha),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Voice input button
                        IconButton(
                            onClick = {
                                if (listening) {
                                    recognizer.stopListening()
                                    listening = false
                                } else {
                                    val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                                    }
                                    recognizer.startListening(intent)
                                    listening = true
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                if (listening) Icons.Default.MicOff else Icons.Default.Mic,
                                "语音输入",
                                tint = if (listening) Color.Red else TextMauve,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            placeholder = { Text("和${characterName}说点什么吧…", color = Color.Gray, fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PinkCherry,
                                unfocusedBorderColor = PinkCherry.copy(alpha = 0.3f),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            maxLines = 3
                        )

                        Spacer(Modifier.width(4.dp))

                        FilledIconButton(
                            onClick = { sendMessage() },
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = PinkCherry,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Send, "发送", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(msg: Message, alpha: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!msg.isUser) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(PinkCherry),
                contentAlignment = Alignment.Center
            ) {
                Text(characterLetter, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (msg.isUser) 16.dp else 4.dp,
                bottomEnd = if (msg.isUser) 4.dp else 16.dp
            ),
            color = (if (msg.isUser) BubbleUser else BubbleXilian).copy(alpha = alpha),
            shadowElevation = 1.dp
        ) {
            Text(
                text = if (msg.isLoading) "⏳ ${msg.text}" else msg.text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = TextMauve,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }

        if (msg.isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(PurpleLavender),
                contentAlignment = Alignment.Center
            ) { Text("你", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
    }
}
