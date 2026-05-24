package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.example.ui.screens.AssistantDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AssistantViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        var showSplash by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            delay(2800)
            showSplash = false
        }

        if (showSplash) {
            SplashScreen()
        } else {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
              val viewModel: AssistantViewModel = viewModel()
              AssistantDashboard(
                  viewModel = viewModel,
                  modifier = Modifier.padding(innerPadding)
              )
            }
        }
      }
    }
  }
}

@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Huawei red logo placeholder using Canvas or Box
        Box(
            modifier = Modifier
                .size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FilterVintage, // closest to red lotus petals
                contentDescription = "Huawei Logo",
                tint = Color(0xFFE50012), // Huawei Red
                modifier = Modifier.size(72.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "HUAWEI",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
        )
        Spacer(modifier = Modifier.height(200.dp)) // push bottom text down
        Text(
            text = "Powered by",
            color = Color.Gray,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
        Text(
            text = "Android",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

