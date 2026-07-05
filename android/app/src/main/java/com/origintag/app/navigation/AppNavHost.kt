package com.origintag.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.origintag.app.feature.dashboard.DashboardScreen
import com.origintag.app.feature.detail.PassportDetailScreen
import com.origintag.app.feature.onboarding.OnboardingScreen
import com.origintag.app.feature.register.RegisterScreen
import com.origintag.app.feature.scan.ScanScreen
import com.origintag.app.feature.transfer.TransferScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val REGISTER = "register"
    const val SCAN = "scan"
    const val PASSPORT_DETAIL = "passport/{tokenId}"
    const val TRANSFER = "transfer/{tokenId}"

    fun passportDetail(tokenId: String) = "passport/$tokenId"
    fun transfer(tokenId: String) = "transfer/$tokenId"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.ONBOARDING) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onLoggedIn = {
                navController.navigate(Routes.DASHBOARD) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onRegisterClick = { navController.navigate(Routes.REGISTER) },
                onScanClick = { navController.navigate(Routes.SCAN) },
                onPassportClick = { tokenId -> navController.navigate(Routes.passportDetail(tokenId)) },
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(onDone = { navController.popBackStack() })
        }

        composable(Routes.SCAN) {
            ScanScreen(onResult = { tokenId ->
                navController.navigate(Routes.passportDetail(tokenId))
            })
        }

        composable(
            Routes.PASSPORT_DETAIL,
            arguments = listOf(navArgument("tokenId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val tokenId = backStackEntry.arguments?.getString("tokenId").orEmpty()
            PassportDetailScreen(
                tokenId = tokenId,
                onTransferClick = { navController.navigate(Routes.transfer(tokenId)) },
            )
        }

        composable(
            Routes.TRANSFER,
            arguments = listOf(navArgument("tokenId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val tokenId = backStackEntry.arguments?.getString("tokenId").orEmpty()
            TransferScreen(
                tokenId = tokenId,
                onDone = { navController.popBackStack(Routes.DASHBOARD, inclusive = false) },
            )
        }
    }
}
