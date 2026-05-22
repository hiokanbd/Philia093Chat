package com.philia093.chat.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.philia093.chat.network.ChatClient
import com.philia093.chat.settings.AppSettings
import kotlinx.coroutines.launch

private val PinkCherry = Color(0xFFFFB7C5)
private val TextMauve = Color(0xFF5E4B66)
private val BgCream = Color(0xFFFFF8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf("") }
    var apiBase by remember { mutableStateOf(AppSettings.DEFAULT_API_BASE) }
    var bgUri by remember { mutableStateOf("") }
    var bgBrightness by remember { mutableFloatStateOf(AppSettings.DEFAULT_BG_BRIGHTNESS) }
    var bubbleAlpha by remember { mutableFloatStateOf(AppSettings.DEFAULT_BUBBLE_ALPHA) }
    var selectedFont by remember { mutableStateOf(POPULAR_FONTS.first()) }
    var avatarUri by remember { mutableStateOf("") }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloading by remember { mutableStateOf(false) }

    // Character management
    var characters by remember { mutableStateOf<List<ChatClient.CharacterInfo>>(emptyList()) }
    var activeCharacter by remember { mutableStateOf<String?>(null) }
    var switchingCharacter by remember { mutableStateOf(false) }

    // Load saved settings + characters
    LaunchedEffect(Unit) {
        AppSettings.apiKeyFlow(context).collect { apiKey = it }
    }
    LaunchedEffect(Unit) {
        val result = ChatClient.getCharacters()
        characters = result.characters
        activeCharacter = result.active
    }
    LaunchedEffect(Unit) {
        AppSettings.apiBaseFlow(context).collect { apiBase = it }
    }
    LaunchedEffect(Unit) {
        AppSettings.bgUriFlow(context).collect { bgUri = it }
    }
    LaunchedEffect(Unit) {
        AppSettings.bgBrightnessFlow(context).collect { bgBrightness = it }
    }
    LaunchedEffect(Unit) {
        AppSettings.bubbleAlphaFlow(context).collect { bubbleAlpha = it }
    }
    LaunchedEffect(Unit) {
        AppSettings.avatarUriFlow(context).collect { avatarUri = it }
    }

    // Image pickers
    val bgPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val path = it.toString()
            bgUri = path
            scope.launch { AppSettings.setBgUri(context, path) }
        }
    }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val path = it.toString()
            avatarUri = path
            scope.launch { AppSettings.setAvatarUri(context, path) }
        }
    }

    Scaffold(
        containerColor = BgCream,
        topBar = {
            TopAppBar(
                title = { Text("设置", color = TextMauve, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TextMauve)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.9f))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ── API 配置 ──
            item {
                SectionHeader("🔑 DeepSeek API 配置")
            }
            item {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("sk-...") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = apiBase,
                    onValueChange = { apiBase = it },
                    label = { Text("API 接口地址") },
                    enabled = false,  // DeepSeek official only
                    supportingText = { Text("仅支持 DeepSeek 官方接口", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
            item {
                Button(
                    onClick = {
                        scope.launch {
                            AppSettings.setApiKey(context, apiKey.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PinkCherry),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("保存 API Key", color = Color.White)
                }
            }

            // ── 角色选择 ──
            item { Spacer(Modifier.height(8.dp)) }
            item { SectionHeader("🎭 角色人格") }
            item {
                Text(
                    "选择一个角色，AI 将以该角色的人格与你对话",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            if (characters.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Text(
                            "未连接到后端，无法加载角色列表",
                            modifier = Modifier.padding(14.dp),
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            items(characters.size) { index ->
                val char = characters[index]
                val isActive = char.name == activeCharacter
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isActive && !switchingCharacter) {
                            scope.launch {
                                switchingCharacter = true
                                val ok = ChatClient.switchCharacter(char.name)
                                if (ok) {
                                    activeCharacter = char.name
                                }
                                switchingCharacter = false
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) PinkCherry.copy(alpha = 0.2f) else Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(PinkCherry, RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                char.avatarLetter,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                char.displayName.ifEmpty { char.name },
                                fontWeight = FontWeight.Medium,
                                color = TextMauve,
                                fontSize = 15.sp
                            )
                            Text(
                                char.description.take(60),
                                fontSize = 12.sp,
                                color = Color.Gray,
                                maxLines = 2
                            )
                        }
                        if (isActive) {
                            Icon(
                                Icons.Default.Check,
                                "当前角色",
                                tint = PinkCherry,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (switchingCharacter && char.name == activeCharacter) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = PinkCherry
                            )
                        }
                    }
                }
            }

            // ── 外观 ──
            item { Spacer(Modifier.height(8.dp)) }
            item { SectionHeader("🎨 外观设置") }

            // Avatar
            item {
                SettingRow("自定义头像") {
                    OutlinedButton(
                        onClick = { avatarPicker.launch("image/*") },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (avatarUri.isNotEmpty()) "已选择" else "选择图片")
                    }
                }
            }

            // Background
            item {
                SettingRow("聊天背景") {
                    OutlinedButton(
                        onClick = { bgPicker.launch("image/*") },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (bgUri.isNotEmpty()) "已选择" else "选择图片")
                    }
                }
            }

            // Background brightness
            item {
                SettingRow("背景亮度") {
                    Text("${(bgBrightness * 100).toInt()}%", color = TextMauve)
                }
                Slider(
                    value = bgBrightness,
                    onValueChange = { bgBrightness = it },
                    onValueChangeFinished = {
                        scope.launch { AppSettings.setBgBrightness(context, bgBrightness) }
                    },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = PinkCherry, activeTrackColor = PinkCherry)
                )
            }

            // Bubble transparency
            item {
                SettingRow("气泡透明度") {
                    Text("${(bubbleAlpha * 100).toInt()}%", color = TextMauve)
                }
                Slider(
                    value = bubbleAlpha,
                    onValueChange = { bubbleAlpha = it },
                    onValueChangeFinished = {
                        scope.launch { AppSettings.setBubbleAlpha(context, bubbleAlpha) }
                    },
                    valueRange = 0.2f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = PinkCherry, activeTrackColor = PinkCherry)
                )
            }

            // ── 字体 ──
            item { Spacer(Modifier.height(8.dp)) }
            item { SectionHeader("🔤 字体选择") }

            items(POPULAR_FONTS.size) { index ->
                val font = POPULAR_FONTS[index]
                val isSelected = selectedFont.name == font.name
                val isReady = FontManager.isDownloaded(font)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedFont = font
                            if (font.fileName.isEmpty()) {
                                scope.launch { AppSettings.setFont(context, font.name, "") }
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) PinkCherry.copy(alpha = 0.2f) else Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(font.name, fontWeight = FontWeight.Medium, color = TextMauve)
                            Text(font.preview, fontSize = 13.sp, color = Color.Gray)
                        }
                        if (font.url.isNotEmpty()) {
                            if (isReady) {
                                Icon(Icons.Default.Check, "已下载", tint = PinkCherry)
                            } else {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            downloading = true
                                            val file = FontManager.downloadFont(font) { progress ->
                                                downloadProgress = progress
                                            }
                                            if (file != null) {
                                                scope.launch { AppSettings.setFont(context, font.name, font.url) }
                                            }
                                            downloading = false
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Download, "下载", tint = TextMauve)
                                }
                            }
                        }
                        if (isSelected) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.Check, "已选", tint = PinkCherry)
                        }
                    }
                }
            }

            // ── 语音模块接口 ──
            item { Spacer(Modifier.height(8.dp)) }
            item { SectionHeader("🎙️ 语音模块（开发中）") }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "TTS 语音合成接口已预留",
                            color = TextMauve,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "实现 com.philia093.chat.voice.TtsInterface 即可接入特定人声模型。\n支持流式合成 / 批量合成 / 音色列表。",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        color = TextMauve,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
fun SettingRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextMauve, fontWeight = FontWeight.Medium)
        content()
    }
}
