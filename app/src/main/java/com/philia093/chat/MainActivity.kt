package com.philia093.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.philia093.chat.ui.ChatScreen
import com.philia093.chat.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var showSettings by remember { mutableStateOf(false) }

            if (showSettings) {
                SettingsScreen(onBack = { showSettings = false })
            } else {
                ChatScreen(onOpenSettings = { showSettings = true })
            }
        }
    }
}
