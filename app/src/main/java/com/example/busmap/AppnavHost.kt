package com.example.busmap

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.busmap.Page.BusRouteScreen
import com.example.busmap.Page.Main
import com.example.busmap.Page.FavoritePage
import com.example.busmap.Page.PersonPage
import com.example.busmap.Page.findTheWay
import com.example.busmap.Page.selectBus
import com.example.busmap.Page.surroundingStation
import com.example.busmap.Page.LoginScreen
import com.example.busmap.Page.RegisterScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "login", // Đặt màn hình đăng nhập là mặc định
        modifier = modifier
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { navController.navigate("main") },
                onNavigateRegister = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { navController.popBackStack(); navController.navigate("main") }
            )
        }
        composable("main") { Main(navController) }
        composable("surroundingstation") { surroundingStation(navController) }
        composable("selectbus") { selectBus(navController) }
        composable("findtheway") { findTheWay(navController) }
        composable("favorite") { FavoritePage(navController) }
        composable("person") { PersonPage(navController) }
        composable(
            route = "busroute/{routeId}",
            arguments = listOf(navArgument("routeId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            BusRouteScreen(
                routeId = backStackEntry.arguments?.getString("routeId"),
                navController = navController
            )
        }
    }
}