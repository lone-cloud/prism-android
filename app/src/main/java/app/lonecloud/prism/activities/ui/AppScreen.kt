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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.lonecloud.prism.PrismPreferences
import app.lonecloud.prism.R
import app.lonecloud.prism.activities.MainViewModel
import app.lonecloud.prism.activities.PreviewFactory
import app.lonecloud.prism.activities.SettingsViewModel
import app.lonecloud.prism.activities.ThemeViewModel
import org.unifiedpush.android.distributor.ipc.subscribeUiActions
import org.unifiedpush.android.distributor.ui.compose.AppBar
import org.unifiedpush.android.distributor.ui.vm.DistribMigrationViewModel

enum class AppScreen(@param:StringRes val title: Int) {
    Intro(R.string.app_name),
    Main(R.string.app_name),
    Settings(R.string.settings),
    ServerConfig(R.string.configure_server),
    AddApp(R.string.add_custom_app_title),
    AppPicker(R.string.select_target_app_title)
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
    val context = LocalContext.current
    val uiActionsFlow = subscribeUiActions(context)
    val prefs = remember { PrismPreferences(context) }
    val startDestination = if (prefs.introCompleted) AppScreen.Main.name else AppScreen.Intro.name

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = AppScreen.valueOf(
        backStackEntry?.destination?.route ?: AppScreen.Main.name
    )
    val migrationViewModel = viewModel<DistribMigrationViewModel>(factory = factory)
    val mainViewModel = viewModel<MainViewModel>(factory = factory)

    Scaffold(
        topBar = {
            if (currentScreen != AppScreen.Intro) {
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
            }
        },
        floatingActionButton = {
            val serverPrefs = PrismPreferences(context)
            if (currentScreen == AppScreen.Main &&
                !serverPrefs.prismServerUrl.isNullOrBlank() &&
                !serverPrefs.prismApiKey.isNullOrBlank()
            ) {
                FloatingActionButton(
                    onClick = {
                        mainViewModel.clearSelectedApp()
                        navController.navigate(AppScreen.AddApp.name)
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_manual_app_content_description))
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(route = AppScreen.Intro.name) {
                val settingsViewModel = viewModel<SettingsViewModel>(factory = factory)
                IntroScreen(
                    onComplete = { url, apiKey ->
                        PrismPreferences(context).introCompleted = true
                        settingsViewModel.savePrismConfig(url, apiKey)
                        navController.navigate(AppScreen.Main.name) {
                            popUpTo(AppScreen.Intro.name) { inclusive = true }
                        }
                    },
                    onSkip = {
                        PrismPreferences(context).introCompleted = true
                        navController.navigate(AppScreen.Main.name) {
                            popUpTo(AppScreen.Intro.name) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = AppScreen.Main.name,
                exitTransition = {
                    when (targetState.destination.route) {
                        AppScreen.Settings.name -> slideOutFrom(Dir.Right)
                        AppScreen.AddApp.name -> slideOutFrom(Dir.Right)
                        else -> fadeOut()
                    }
                },
                popEnterTransition = {
                    when (initialState.destination.route) {
                        AppScreen.Settings.name -> slideInTo(Dir.Right)
                        AppScreen.AddApp.name -> slideInTo(Dir.Right)
                        else -> fadeIn()
                    }
                }
            ) {
                MainScreen(
                    mainViewModel,
                    migrationViewModel,
                    uiActionsFlow
                )
            }
            composable(
                route = AppScreen.Settings.name,
                enterTransition = { slideInTo(Dir.Left) },
                exitTransition = {
                    when (targetState.destination.route) {
                        AppScreen.ServerConfig.name -> slideOutFrom(Dir.Right)
                        else -> fadeOut()
                    }
                },
                popEnterTransition = {
                    when (initialState.destination.route) {
                        AppScreen.ServerConfig.name -> slideInTo(Dir.Right)
                        else -> fadeIn()
                    }
                },
                popExitTransition = { slideOutFrom(Dir.Left) }
            ) {
                val vm = viewModel<SettingsViewModel>(factory = factory)
                SettingsScreen(
                    vm,
                    themeViewModel,
                    migrationViewModel,
                    onNavigateToServerConfig = {
                        navController.navigate(AppScreen.ServerConfig.name)
                    }
                )
            }
            composable(
                route = AppScreen.ServerConfig.name,
                enterTransition = { slideInTo(Dir.Left) },
                popEnterTransition = { slideInTo(Dir.Right) },
                popExitTransition = { slideOutFrom(Dir.Left) }
            ) {
                val settingsEntry = remember(it) {
                    navController.getBackStackEntry(AppScreen.Settings.name)
                }
                val vm = viewModel<SettingsViewModel>(
                    viewModelStoreOwner = settingsEntry,
                    factory = factory
                )
                ServerConfigScreen(
                    initialUrl = vm.state.prismServerUrl,
                    initialApiKey = vm.state.prismApiKey,
                    onNavigateBack = { navController.navigateUp() },
                    onSave = { url, apiKey -> vm.savePrismConfig(url, apiKey) }
                )
            }
            composable(
                route = AppScreen.AddApp.name,
                enterTransition = { slideInTo(Dir.Left) },
                exitTransition = {
                    when (targetState.destination.route) {
                        AppScreen.AppPicker.name -> slideOutFrom(Dir.Right)
                        else -> fadeOut()
                    }
                },
                popEnterTransition = {
                    when (initialState.destination.route) {
                        AppScreen.AppPicker.name -> slideInTo(Dir.Right)
                        else -> fadeIn()
                    }
                },
                popExitTransition = { slideOutFrom(Dir.Left) }
            ) {
                AddAppScreen(
                    selectedApp = mainViewModel.selectedApp,
                    prefilledName = mainViewModel.prefilledName,
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToAppPicker = {
                        navController.navigate(AppScreen.AppPicker.name)
                    },
                    onConfirm = { name, packageName, description ->
                        mainViewModel.addManualApp(name, packageName, description)
                        mainViewModel.prefilledName = null
                    }
                )
            }
            composable(
                route = AppScreen.AppPicker.name,
                enterTransition = { slideInTo(Dir.Left) },
                popEnterTransition = { slideInTo(Dir.Right) },
                popExitTransition = { slideOutFrom(Dir.Left) }
            ) {
                AppPickerScreen(
                    apps = mainViewModel.mainUiState.installedApps,
                    onNavigateBack = { navController.navigateUp() },
                    onSelect = { app ->
                        mainViewModel.selectApp(app)
                    },
                    onSelectPrismApp = { appName ->
                        mainViewModel.prefilledName = appName
                    }
                )
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
