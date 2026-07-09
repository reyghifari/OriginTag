package com.origintag.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.origintag.app.feature.dashboard.DashboardScreen
import com.origintag.app.feature.detail.PassportDetailScreen
import com.origintag.app.feature.explore.ExploreScreen
import com.origintag.app.feature.marketplace.MarketplaceScreen
import com.origintag.app.feature.onboarding.OnboardingScreen
import com.origintag.app.feature.profile.ProfileScreen
import com.origintag.app.feature.register.RegisterScreen
import com.origintag.app.feature.scan.ScanScreen
import com.origintag.app.feature.transfer.TransferScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val REGISTER = "register"
    const val SCAN = "scan"
    const val PASSPORT_DETAIL = "passport/{tokenId}"
    const val TRANSFER = "transfer/{tokenId}"

    fun passportDetail(tokenId: String) = "passport/$tokenId"
    fun transfer(tokenId: String) = "transfer/$tokenId"
}

/** Tab bottom-nav dalam MAIN */
private enum class Tab(val route: String, val label: String, val emoji: String) {
    DASHBOARD("tab_dashboard", "Barang", "👜"),
    EXPLORE("tab_explore", "Explore", "🔍"),
    MARKETPLACE("tab_market", "Market", "🏷️"),
    PROFILE("tab_profile", "Profil", "👤"),
}

@Composable
fun AppNavHost() {
    val rootNav = rememberNavController()

    NavHost(navController = rootNav, startDestination = Routes.ONBOARDING) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onLoggedIn = {
                rootNav.navigate(Routes.MAIN) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }

        composable(Routes.MAIN) { MainScaffold(rootNav) }

        composable(Routes.REGISTER) {
            RegisterScreen(onDone = { rootNav.popBackStack() })
        }

        composable(Routes.SCAN) {
            ScanScreen(onResult = { tokenId ->
                rootNav.navigate(Routes.passportDetail(tokenId))
            })
        }

        composable(
            Routes.PASSPORT_DETAIL,
            arguments = listOf(navArgument("tokenId") { type = NavType.StringType }),
        ) { entry ->
            val tokenId = entry.arguments?.getString("tokenId").orEmpty()
            PassportDetailScreen(
                tokenId = tokenId,
                onTransferClick = { rootNav.navigate(Routes.transfer(tokenId)) },
            )
        }

        composable(
            Routes.TRANSFER,
            arguments = listOf(navArgument("tokenId") { type = NavType.StringType }),
        ) { entry ->
            val tokenId = entry.arguments?.getString("tokenId").orEmpty()
            TransferScreen(tokenId = tokenId, onDone = { rootNav.popBackStack() })
        }
    }
}

/** Scaffold dengan bottom navigation + NavHost bersarang untuk tab. */
@Composable
private fun MainScaffold(rootNav: NavHostController) {
    val tabNav = rememberNavController()
    val backStack by tabNav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            tabNav.navigate(tab.route) {
                                popUpTo(tabNav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(tab.emoji) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = tabNav,
            startDestination = Tab.DASHBOARD.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.DASHBOARD.route) {
                DashboardScreen(
                    onRegisterClick = { rootNav.navigate(Routes.REGISTER) },
                    onScanClick = { rootNav.navigate(Routes.SCAN) },
                    onPassportClick = { tokenId -> rootNav.navigate(Routes.passportDetail(tokenId)) },
                )
            }
            composable(Tab.EXPLORE.route) {
                ExploreScreen(onItemClick = { rootNav.navigate(Routes.passportDetail(it)) })
            }
            composable(Tab.MARKETPLACE.route) {
                MarketplaceScreen(onItemClick = { rootNav.navigate(Routes.passportDetail(it)) })
            }
            composable(Tab.PROFILE.route) {
                ProfileScreen(onLoggedOut = {
                    rootNav.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                })
            }
        }
    }
}
