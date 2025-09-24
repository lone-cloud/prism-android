package org.unifiedpush.distributor.sunup.activities.ui

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.unifiedpush.distributor.sunup.R
import org.unifiedpush.distributor.sunup.activities.MainViewModel
import org.unifiedpush.distributor.sunup.activities.PreviewFactory

enum class AppScreen(@StringRes val title: Int) {
    Main(R.string.app_name),
    Settings(R.string.app_name)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultTopBar(
    currentScreen: AppScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) = AppBar(
    currentScreen.title,
    canNavigateBack,
    navigateUp,
    modifier
)

@Composable
inline fun <reified VM : ViewModel> sharedViewModel(
    navController: NavHostController,
    screen: AppScreen,
    factory: ViewModelProvider.Factory
): VM? {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    return if (currentBackStackEntry?.destination?.route == screen.name) {
        val parentEntry = remember(currentBackStackEntry) {
            navController.getBackStackEntry(screen.name)
        }
        viewModel(parentEntry, factory = factory)
    } else {
        null
    }
}

@Composable
fun App(factory: ViewModelProvider.Factory, navController: NavHostController = rememberNavController()) {
    // Get current back stack entry
    val backStackEntry by navController.currentBackStackEntryAsState()
    // Get the name of the current screen
    val currentScreen = AppScreen.valueOf(
        backStackEntry?.destination?.route ?: AppScreen.Main.name
    )

    Scaffold(
        topBar = {
            when (currentScreen) {
                AppScreen.Main -> {
                    sharedViewModel<MainViewModel>(
                        navController,
                        currentScreen,
                        factory
                    )?.let {
                        MainAppBar(it)
                    }
                }
                else -> null
            } ?: DefaultTopBar(
                currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppScreen.Main.name,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
        ) {
            composable(route = AppScreen.Main.name) {
                sharedViewModel<MainViewModel>(navController, currentScreen, factory)?.let {
                    MainUiContent(it)
                }
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview
@Composable
fun PreviewApp() = App(
    factory = PreviewFactory(LocalContext.current)
)
