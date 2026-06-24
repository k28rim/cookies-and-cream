package com.cookiesandcream.queuebuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.cookiesandcream.queuebuddy.ui.theme.QueueBuddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as QueueBuddyApp).container
        setContent {
            QueueBuddyTheme {
                AppNav(container)
            }
        }
    }
}

@Composable
private fun AppNav(container: AppContainer) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
            HomeScreen(vm, onOpenLocation = { id -> navController.navigate("location/$id") })
        }
        composable(
            route = "location/{locationId}",
            arguments = listOf(navArgument("locationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("locationId").orEmpty()
            val vm: LocationDetailViewModel =
                viewModel(factory = LocationDetailViewModel.factory(container, id))
            LocationDetailScreen(vm, onBack = { navController.popBackStack() })
        }
    }
}
