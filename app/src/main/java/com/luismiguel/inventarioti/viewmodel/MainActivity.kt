package com.luismiguel.inventarioti.viewmodel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.luismiguel.inventarioti.ui.screen.login.LoginForm
import com.luismiguel.inventarioti.ui.screen.main.MainScreen
import com.luismiguel.inventarioti.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            val navController = rememberNavController()

            AppTheme(darkTheme = isDarkMode) {
                NavHost(navController = navController, startDestination = "login") {
                    composable("login") {
                        LoginForm(
                            navController = navController,
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { isDarkMode = !isDarkMode }
                        )
                    }
                    composable("main") {
                        MainScreen(
                            navController = navController,
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { isDarkMode = !isDarkMode }
                        )
                    }
                }
            }
        }
    }
}
