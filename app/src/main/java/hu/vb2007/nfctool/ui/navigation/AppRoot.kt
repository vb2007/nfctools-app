package hu.vb2007.nfctool.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import hu.vb2007.nfctool.ui.home.HomeScreen
import hu.vb2007.nfctool.ui.read.ReadScreen
import hu.vb2007.nfctool.ui.settings.SettingsScreen
import hu.vb2007.nfctool.ui.write.WriteScreen

@Composable
fun AppRoot() {
    val backStack = rememberNavBackStack(HomeRoute)
    val currentRoute = backStack.lastOrNull()
    val showBottomBar = backStack.size == 1 && (currentRoute is HomeRoute || currentRoute is SettingsRoute)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentRoute = currentRoute,
                    onHomeClick = {
                        backStack.clear()
                        backStack.add(HomeRoute)
                    },
                    onSettingsClick = {
                        backStack.clear()
                        backStack.add(SettingsRoute)
                    },
                )
            }
        },
    ) { paddingValues ->
        NavDisplay(
            modifier = Modifier.padding(paddingValues),
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<HomeRoute> {
                    HomeScreen(
                        onReadClick = { backStack.add(ReadRoute) },
                        onWriteClick = { backStack.add(WriteRoute) },
                    )
                }
                entry<SettingsRoute> { SettingsScreen() }
                entry<ReadRoute> { ReadScreen(onBack = { backStack.removeLastOrNull() }) }
                entry<WriteRoute> { WriteScreen(onBack = { backStack.removeLastOrNull() }) }
            },
        )
    }
}
