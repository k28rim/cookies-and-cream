package com.cookiesandcream.queuebuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cookiesandcream.queuebuddy.ui.detail.LocationDetailScreen
import com.cookiesandcream.queuebuddy.ui.detail.LocationDetailViewModel
import com.cookiesandcream.queuebuddy.ui.home.HomeScreen
import com.cookiesandcream.queuebuddy.ui.home.HomeViewModel
import com.cookiesandcream.queuebuddy.ui.moderation.ModerationScreen
import com.cookiesandcream.queuebuddy.ui.moderation.ModerationViewModel
import com.cookiesandcream.queuebuddy.ui.theme.QueueBuddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as QueueBuddyApp).container
        setContent {
            QueueBuddyTheme {
                QueueBuddyNavHost(container)
            }
        }
    }
}

@Composable
fun QueueBuddyNavHost(container: AppContainer) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
            HomeScreen(
                viewModel = viewModel,
                onOpenLocation = { id -> navController.navigate("location/$id") },
                onOpenModeration = { navController.navigate("moderation") }
            )
        }
        composable(
            route = "location/{locationId}",
            arguments = listOf(navArgument("locationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val locationId = backStackEntry.arguments?.getString("locationId").orEmpty()
            val viewModel: LocationDetailViewModel = viewModel(
                factory = LocationDetailViewModel.factory(container, locationId)
            )
            LocationDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("moderation") {
            val viewModel: ModerationViewModel =
                viewModel(factory = ModerationViewModel.factory(container))
            ModerationScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
