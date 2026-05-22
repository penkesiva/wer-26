package com.golfcues.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.golfcues.app.ui.detail.SessionDetailScreen
import com.golfcues.app.ui.history.SessionHistoryScreen
import com.golfcues.app.ui.home.HomeScreen
import com.golfcues.app.ui.live.LiveGolfModeScreen
import com.golfcues.app.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val LIVE = "live"
    const val HISTORY = "history"
    const val DETAIL = "detail/{sessionId}"
    const val SETTINGS = "settings"

    fun detail(sessionId: String) = "detail/$sessionId"
}

@Composable
fun GolfCuesNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateLive = { navController.navigate(Routes.LIVE) },
                onNavigateHistory = { navController.navigate(Routes.HISTORY) },
                onNavigateSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.LIVE) {
            LiveGolfModeScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.HISTORY) {
            SessionHistoryScreen(
                onSessionClick = { id -> navController.navigate(Routes.detail(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            SessionDetailScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
