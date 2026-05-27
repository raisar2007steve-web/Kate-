package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.AssistantViewModel

data class AgentProfile(
    val name: String,
    val modelId: String,
    val roleDescription: String,
    val icon: ImageVector,
    val color: Color
)

val MULTI_AGENTS = listOf(
    AgentProfile(
        name = "Code Master",
        modelId = "meta/llama-3.1-405b-instruct",
        roleDescription = "You are an elite software architecture expert and code writer.",
        icon = Icons.Default.Code,
        color = Color(0xFFE91E63)
    ),
    AgentProfile(
        name = "Creative Writer",
        modelId = "mistralai/mixtral-8x22b-instruct-v0.1",
        roleDescription = "You are a highly creative novelist and ideator.",
        icon = Icons.Default.Create,
        color = Color(0xFFFF9800)
    ),
    AgentProfile(
        name = "Data Analyst",
        modelId = "meta/llama-3.1-70b-instruct",
        roleDescription = "You are a meticulous data scientist and statistical analyst.",
        icon = Icons.Default.Science,
        color = Color(0xFF2196F3)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiAgentsAppView(viewModel: AssistantViewModel) {
    var selectedAgentIndex by remember { mutableIntStateOf(0) }
    val activeAgent = MULTI_AGENTS[selectedAgentIndex]
    
    val msgs by viewModel.multiAgentMessages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isMultiAgentLoading.collectAsStateWithLifecycle()
    
    var inputText by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2C2C2E))
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "AI AGENTS SYNDICATE",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "NVIDIA NIM Multi-Model Platform",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
        
        // Agent Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MULTI_AGENTS.forEachIndexed { index, agent ->
                val isSelected = index == selectedAgentIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) agent.color.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) agent.color else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedAgentIndex = index }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(agent.icon, contentDescription = null, tint = if (isSelected) agent.color else Color.Gray, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(agent.name, color = if (isSelected) agent.color else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // Chat Area
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            reverseLayout = true
        ) {
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        CircularProgressIndicator(color = activeAgent.color, modifier = Modifier.size(24.dp))
                    }
                }
            }
            items(msgs.reversed()) { msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!isUser) {
                        // Bot Avatar
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(activeAgent.color.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(activeAgent.icon, contentDescription = null, tint = activeAgent.color, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(
                                topStart = 16.dp, 
                                topEnd = 16.dp, 
                                bottomStart = if (isUser) 16.dp else 4.dp, 
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            ))
                            .background(if (isUser) Color(0xFF0A84FF) else Color(0xFF2C2C2E))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = msg.content,
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                    
                    if (isUser) {
                        Spacer(modifier = Modifier.width(32.dp)) // padding from right edge equivalent to avatar if needed
                    }
                }
            }
        }
        
        // Input Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color(0xFF2C2C2E), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask ${activeAgent.name}...", color = Color.Gray, fontSize = 14.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                maxLines = 3
            )
            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !isLoading) {
                        viewModel.sendMultiAgentMessage(
                            prompt = inputText,
                            agentModel = activeAgent.modelId,
                            agentRole = activeAgent.roleDescription
                        )
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(activeAgent.color)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}
