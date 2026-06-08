package com.callrecorder.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.callrecorder.app.presentation.screens.home.HomeScreen
import com.callrecorder.app.presentation.screens.onboarding.PermissionsOnboardingScreen
import com.callrecorder.app.presentation.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("permissions_onboarding")
    object Home : Screen("home")
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenSystemSettings: () -> Unit
) {
    val navController = rememberNavController()
    val startDestination = if (hasPermissions) Screen.Home.route else Screen.Onboarding.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Onboarding.route) {
            PermissionsOnboardingScreen(
                onRequestPermissions = onRequestPermissions,
                onOpenSettings = onOpenSystemSettings,
                onContinue = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
