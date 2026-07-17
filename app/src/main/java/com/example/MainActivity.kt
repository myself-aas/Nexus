package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.presentation.AgentsDestination
import com.example.presentation.ChatDestination
import com.example.presentation.ConversationsDestination
import com.example.presentation.SettingsDestination
import com.example.presentation.SkillsDestination
import com.example.presentation.agents.AgentsScreen
import com.example.presentation.chat.ChatScreen
import com.example.presentation.conversations.ConversationsScreen
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.skills.SkillsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.GlassGradientBackground
import com.example.ui.theme.GlassBottomNav
import com.example.ui.theme.GlassBottomNavItem
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                
                val navItems = listOf(
                    NavItem("Chat", Icons.Default.Chat, ChatDestination, ChatDestination::class),
                    NavItem("Library", Icons.Default.History, ConversationsDestination, ConversationsDestination::class),
                    NavItem("Skills", Icons.Default.Extension, SkillsDestination, SkillsDestination::class),
                    NavItem("Agents", Icons.Default.SmartToy, AgentsDestination, AgentsDestination::class),
                    NavItem("Settings", Icons.Default.Settings, SettingsDestination, SettingsDestination::class)
                )

                GlassGradientBackground {
                    Scaffold(
                        modifier = Modifier.fillMaxSize().testTag("main_scaffold"),
                        containerColor = Color.Transparent,
                        bottomBar = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .navigationBarsPadding(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                GlassBottomNav(
                                    modifier = Modifier.weight(1f).testTag("bottom_nav_bar")
                                ) {
                                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                                    val currentDestination = navBackStackEntry?.destination

                                    // First 4 items in the pill
                                    navItems.take(4).forEach { item ->
                                        val selected = currentDestination?.hasRoute(item.clazz) == true
                                        GlassBottomNavItem(
                                            modifier = Modifier.testTag("nav_item_${item.name.lowercase()}"),
                                            selected = selected,
                                            onClick = {
                                                navController.navigate(item.destination) {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = item.icon,
                                            label = item.name
                                        )
                                    }
                                }

                                // Settings button decoupled
                                val isSettingsSelected = navController.currentBackStackEntryAsState().value?.destination?.hasRoute(SettingsDestination::class) == true
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            navController.navigate(SettingsDestination) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                        .background(if (isSystemInDarkTheme()) Color(0xFF171B26).copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.05f))
                                        .border(1.dp, if (isSystemInDarkTheme()) Color(0xFF2E3B5E) else Color.Black.copy(alpha = 0.05f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = if (isSettingsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = ChatDestination,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            composable<ChatDestination> {
                                ChatScreen()
                            }
                            composable<ConversationsDestination> {
                                ConversationsScreen()
                            }
                            composable<SkillsDestination> {
                                SkillsScreen()
                            }
                            composable<AgentsDestination> {
                                AgentsScreen()
                            }
                            composable<SettingsDestination> {
                                SettingsScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}

data class NavItem<T : Any>(
    val name: String,
    val icon: ImageVector,
    val destination: T,
    val clazz: kotlin.reflect.KClass<T>
)
