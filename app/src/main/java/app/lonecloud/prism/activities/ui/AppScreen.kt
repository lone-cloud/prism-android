package app.lonecloud.prism.activities.ui

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.lonecloud.prism.R
import app.lonecloud.prism.activities.DistribMigrationViewModel
import app.lonecloud.prism.activities.MainViewModel
import app.lonecloud.prism.activities.PreviewFactory
import app.lonecloud.prism.activities.SettingsViewModel
import app.lonecloud.prism.activities.ThemeViewModel
import org.unifiedpush.android.distributor.ui.R as LibR
import org.unifiedpush.android.distributor.ui.compose.AppBar

enum class AppScreen(@param:StringRes val title: Int) {
    Main(R.string.app_name),
    Settings(LibR.string.settings)
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

private enum class Dir {
    Left,
    Right
}

private fun Dir.transform(it: Int): Int = when (this) {
    Dir.Left -> it
    Dir.Right -> -it
}

private fun slideInTo(dir: Dir): EnterTransition = slideInHorizontally(
    animationSpec = tween(durationMillis = 200)
) { dir.transform(it) } + fadeIn(initialAlpha = 1f)

private fun slideOutFrom(dir: Dir): ExitTransition = slideOutHorizontally(
    animationSpec = tween(durationMillis = 200)
) { dir.transform(it) } + fadeOut(targetAlpha = 1f)

@Composable
fun App(
    factory: ViewModelProvider.Factory,
    themeViewModel: ThemeViewModel = viewModel<ThemeViewModel>(factory = factory),
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = AppScreen.valueOf(
        backStackEntry?.destination?.route ?: AppScreen.Main.name
    )
    // shared with all views, no need to scope it
    val migrationViewModel = viewModel<DistribMigrationViewModel>(factory = factory)
    val mainViewModel = viewModel<MainViewModel>(factory = factory)

    Scaffold(
        topBar = {
            when (currentScreen) {
                AppScreen.Main -> {
                    MainAppBarOrSelection(
                        mainViewModel,
                        onGoToSettings = {
                            navController.navigate(AppScreen.Settings.name)
                        }
                    )
                }
                else -> null
            } ?: DefaultTopBar(
                currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() }
            )
        },
        floatingActionButton = {
            if (currentScreen == AppScreen.Main) {
                FloatingActionButton(
                    onClick = { mainViewModel.showAddAppDialog() }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Manual App")
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppScreen.Main.name,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(
                route = AppScreen.Main.name,
                exitTransition = {
                    when (targetState.destination.route) {
                        AppScreen.Settings.name -> slideOutFrom(
                            Dir.Right
                        )
                        else -> fadeOut()
                    }
                },
                popEnterTransition = {
                    when (initialState.destination.route) {
                        AppScreen.Settings.name -> slideInTo(Dir.Right)
                        else -> fadeIn()
                    }
                }
            ) {
                MainScreen(
                    mainViewModel,
                    migrationViewModel
                )
            }
            composable(
                route = AppScreen.Settings.name,
                enterTransition = { slideInTo(Dir.Left) },
                popExitTransition = { slideOutFrom(Dir.Left) }
            ) {
                val vm = viewModel<SettingsViewModel>(factory = factory)
                SettingsScreen(vm, themeViewModel, migrationViewModel)
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
