package com.tasha.recyclify

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

class SplashScreen : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            SplashScreenUI {
                // 🔍 Check Remember Me + FirebaseAuth
                val prefs = getSharedPreferences("RecyclifyPrefs", Context.MODE_PRIVATE)
                val rememberMe = prefs.getBoolean("rememberMe", false)

                if (auth.currentUser != null && rememberMe) {

                    val intent = Intent(this, MainActivity::class.java).apply {
                        putExtra("uid", auth.currentUser?.uid)
                    }
                    this.startActivity(intent)
                } else {
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                finish()
            }
        }
    }
}

@Composable
fun SplashScreenUI(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000) // ⏳ 2s splash delay
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE8FBE8), Color.White)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "RecycliFy",
            style = TextStyle(
                fontSize = 36.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Cursive
            ),
            color = Color.Black
        )
    }
}


