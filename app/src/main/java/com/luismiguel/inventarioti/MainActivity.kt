package com.luismiguel.inventarioti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.luismiguel.inventarioti.home.LoginForm
import com.luismiguel.inventarioti.home.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            
            val navController = rememberNavController()

            // Definición de la navegación
            NavHost(navController = navController, startDestination = "login") {
                composable("login") { LoginForm(navController) }
                composable("main") { MainScreen(navController = navController) }
            }
        }
    }
}