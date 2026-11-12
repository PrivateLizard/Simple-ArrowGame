package com.arrowpuzzle.game.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arrowpuzzle.game.data.AppContainer
import com.arrowpuzzle.game.game.LevelCatalog
import com.arrowpuzzle.game.ui.components.LocalFrameSeedSource
import kotlin.random.Random
import com.arrowpuzzle.game.ui.screens.CustomModeScreen
import com.arrowpuzzle.game.ui.screens.CustomModeWarningDialog
import com.arrowpuzzle.game.ui.screens.DefeatScreen
import com.arrowpuzzle.game.ui.screens.FIRST_START_FRAME_SEED
import com.arrowpuzzle.game.ui.screens.FirstStartScreen
import com.arrowpuzzle.game.ui.screens.GameScreen
import com.arrowpuzzle.game.ui.screens.HomeScreen
import com.arrowpuzzle.game.ui.screens.LevelSelectScreen
import com.arrowpuzzle.game.ui.screens.SettingsScreen
import com.arrowpuzzle.game.ui.screens.TestModeScreen
import com.arrowpuzzle.game.ui.screens.VictoryScreen
import com.arrowpuzzle.game.ui.viewmodel.GameViewModel
import com.arrowpuzzle.game.ui.viewmodel.HomeViewModel
import com.arrowpuzzle.game.ui.viewmodel.LevelSelectViewModel
import com.arrowpuzzle.game.ui.viewmodel.SettingsViewModel
import com.arrowpuzzle.game.ui.viewmodel.SimpleViewModelFactory

@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()
    // Read once: the start destination must not change while the app runs.
    val startDestination = remember(container) {
        val firstRun = !container.settingsRepository.settings.value.firstRunDone &&
            container.scoreRepository.bestScores.value.values.none { it > 0 } &&
            container.scoreRepository.highestUnlocked.value <= 1
        if (firstRun) AppRoutes.FIRST_START else AppRoutes.HOME
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(AppRoutes.FIRST_START) {
            FirstStartScreen(
                onChoose = { nightMode ->
                    container.settingsRepository.completeFirstRun(nightMode)
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.FIRST_START) { inclusive = true }
                    }
                },
            )
        }

        composable(AppRoutes.HOME) {
            val viewModel: HomeViewModel = viewModel(
                factory = remember(container) {
                    SimpleViewModelFactory {
                        HomeViewModel(container.scoreRepository, container.progressRepository)
                    }
                },
            )
            val campaignScore by viewModel.campaignScoreTotal.collectAsState()
            val highestUnlocked by viewModel.highestUnlocked.collectAsState()
            val completedLevels by viewModel.completedLevels.collectAsState()
            val savedGame by viewModel.savedGame.collectAsState()
            val homeSettings by container.settingsRepository.settings.collectAsState()

            // Right after the first-start screen Home keeps its arrow frames,
            // so nothing jumps when it takes over from the preview.
            val frameSeeds = remember {
                if (startDestination == AppRoutes.FIRST_START) {
                    Random(FIRST_START_FRAME_SEED)
                } else {
                    Random.Default
                }
            }
            CompositionLocalProvider(LocalFrameSeedSource provides frameSeeds) {
                HomeScreen(
                    highScore = campaignScore,
                    completedLevels = completedLevels,
                    highestUnlocked = highestUnlocked.coerceAtMost(LevelCatalog.MAX_LEVEL),
                    resumeLevel = savedGame?.config?.levelId,
                    onResume = {
                        savedGame?.let { game ->
                            container.progressRepository.requestResume()
                            navController.navigate(AppRoutes.game(game.config)) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onPlay = {
                        if (highestUnlocked <= 1) {
                            // Only level 1 is open: the level list has nothing to
                            // choose from, so go straight into it.
                            navController.navigate(
                                AppRoutes.game(
                                    LevelCatalog.defaultConfig(
                                        levelId = 1,
                                        maxFailures = homeSettings.maxFailures,
                                    ),
                                ),
                            ) { launchSingleTop = true }
                        } else {
                            navController.navigate(AppRoutes.LEVELS) { launchSingleTop = true }
                        }
                    },
                    onCustom = {
                        navController.navigate(AppRoutes.CUSTOM) { launchSingleTop = true }
                    },
                    onSettings = {
                        navController.navigate(AppRoutes.SETTINGS) { launchSingleTop = true }
                    },
                    onTest = {
                        navController.navigate(AppRoutes.TEST) { launchSingleTop = true }
                    },
                    isAllUnlocked = highestUnlocked >= LevelCatalog.MAX_LEVEL,
                    onToggleUnlockAll = { unlocked ->
                        viewModel.setAllLevelsUnlocked(unlocked)
                    },
                    onUnlockLevelCount = { count ->
                        viewModel.setLevelsUnlocked(count)
                    },
                    devModeEnabled = homeSettings.devModeEnabled,
                    devWinButtons = homeSettings.devWinButtons,
                    onDevWinButtonsChange = container.settingsRepository::setDevWinButtons,
                    maxPoints = if (homeSettings.devModeEnabled && homeSettings.devHomeMaxPoints) {
                        viewModel.campaignMaxTotal()
                    } else {
                        null
                    },
                    devHomeMaxPoints = homeSettings.devHomeMaxPoints,
                    onDevHomeMaxPointsChange = container.settingsRepository::setDevHomeMaxPoints,
                )
            }
        }

        composable(AppRoutes.LEVELS) {
            val viewModel: LevelSelectViewModel = viewModel(
                factory = remember(container) {
                    SimpleViewModelFactory {
                        LevelSelectViewModel(container.scoreRepository, container.settingsRepository)
                    }
                },
            )
            val levels by viewModel.levels.collectAsState()

            LevelSelectScreen(
                levels = levels,
                onBack = { navController.popBackStack() },
                onLevelSelected = { levelId ->
                    navController.navigate(AppRoutes.game(viewModel.configFor(levelId)))
                },
            )
        }

        composable(AppRoutes.SETTINGS) {
            val viewModel: SettingsViewModel = viewModel(
                factory = remember(container) {
                    SimpleViewModelFactory {
                        SettingsViewModel(container.settingsRepository, container.scoreRepository)
                    }
                },
            )
            val settings by viewModel.settings.collectAsState()

            SettingsScreen(
                settings = settings,
                onMaxFailuresChange = viewModel::setMaxFailures,
                onNightModeChange = viewModel::setNightMode,
                onSoundChange = viewModel::setSoundEnabled,
                onHapticChange = viewModel::setHapticEnabled,
                onHintsChange = viewModel::setHintsEnabled,
                onLanguageChange = viewModel::setLanguage,
                onBackupChange = viewModel::setBackupEnabled,
                onDevModeChange = viewModel::setDevModeEnabled,
                onResetProgress = viewModel::resetProgress,
                onBack = { navController.popBackStack() },
            )
        }

        composable(AppRoutes.TEST) {
            val testSettings by container.settingsRepository.settings.collectAsState()
            TestModeScreen(
                maxFailures = testSettings.maxFailures,
                onBack = { navController.popBackStack() },
                onStart = { config ->
                    navController.navigate(AppRoutes.game(config))
                },
            )
        }

        composable(AppRoutes.CUSTOM) {
            val customSettings by container.settingsRepository.settings.collectAsState()
            CustomModeScreen(
                onBack = { navController.popBackStack() },
                onStart = { config ->
                    navController.navigate(AppRoutes.game(config))
                },
            )
            if (!customSettings.customWarningSeen) {
                CustomModeWarningDialog(
                    onConfirm = container.settingsRepository::markCustomWarningSeen,
                )
            }
        }

        composable(
            route = AppRoutes.GAME,
            arguments = gameArguments(),
        ) { entry ->
            val config = AppRoutes.configFrom(entry.arguments)
            if (config == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                Box(Modifier.fillMaxSize())
                return@composable
            }
            val viewModel: GameViewModel = viewModel(
                factory = remember(entry, config) {
                    SimpleViewModelFactory {
                        GameViewModel(
                            config = config,
                            levelGenerator = container.levelGenerator,
                            settingsRepository = container.settingsRepository,
                            scoreRepository = container.scoreRepository,
                            progressRepository = container.progressRepository,
                        )
                    }
                },
            )

            val gameSettings by container.settingsRepository.settings.collectAsState()
            GameScreen(
                viewModel = viewModel,
                soundEffects = container.soundEffects,
                hintsEnabled = gameSettings.hintsEnabled,
                devWinButtons = gameSettings.devModeEnabled && gameSettings.devWinButtons,
                onExit = { navController.popBackStack() },
                onVictory = { result ->
                    navController.navigate(AppRoutes.victory(result)) {
                        popUpTo(AppRoutes.GAME) { inclusive = true }
                    }
                },
                onDefeat = { state, levelConfig ->
                    navController.navigate(AppRoutes.defeat(state, levelConfig)) {
                        popUpTo(AppRoutes.GAME) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = AppRoutes.VICTORY,
            arguments = victoryArguments(),
        ) { entry ->
            val result = AppRoutes.victoryResultFrom(entry.arguments)
            if (result == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                Box(Modifier.fillMaxSize())
                return@composable
            }

            VictoryScreen(
                result = result,
                onNextLevel = {
                    val nextConfig = LevelCatalog.defaultConfig(
                        levelId = result.config.levelId + 1,
                        maxFailures = container.settingsRepository.settings.value.maxFailures,
                    )
                    navController.navigate(AppRoutes.game(nextConfig)) {
                        popUpTo(AppRoutes.VICTORY) { inclusive = true }
                    }
                },
                onRetry = {
                    navController.navigate(AppRoutes.game(result.config)) {
                        popUpTo(AppRoutes.VICTORY) { inclusive = true }
                    }
                },
                onLevels = {
                    navController.navigate(AppRoutes.LEVELS) {
                        popUpTo(AppRoutes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onHome = {
                    navController.popBackStack(AppRoutes.HOME, inclusive = false)
                },
            )
        }

        composable(
            route = AppRoutes.DEFEAT,
            arguments = defeatArguments(),
        ) { entry ->
            val data = AppRoutes.defeatDataFrom(entry.arguments)
            if (data == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                Box(Modifier.fillMaxSize())
                return@composable
            }

            DefeatScreen(
                reason = data.reason,
                failures = data.failures,
                elapsedSeconds = data.elapsedSeconds,
                config = data.config,
                onRetry = {
                    navController.navigate(AppRoutes.game(data.config)) {
                        popUpTo(AppRoutes.DEFEAT) { inclusive = true }
                    }
                },
                onLevels = {
                    navController.navigate(AppRoutes.LEVELS) {
                        popUpTo(AppRoutes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onHome = {
                    navController.popBackStack(AppRoutes.HOME, inclusive = false)
                },
            )
        }
    }
}

private fun gameArguments(): List<NamedNavArgument> = levelConfigArguments()

private fun victoryArguments(): List<NamedNavArgument> = levelConfigArguments() + listOf(
    navArgument("points") { type = NavType.IntType },
    navArgument("timeMillis") { type = NavType.LongType },
    navArgument("failures") { type = NavType.IntType },
    navArgument("newBest") { type = NavType.BoolType },
    navArgument("arrowsUsed") { type = NavType.IntType },
)

private fun defeatArguments(): List<NamedNavArgument> = levelConfigArguments() + listOf(
    navArgument("reason") { type = NavType.StringType },
    navArgument("failures") { type = NavType.IntType },
    navArgument("timeMillis") { type = NavType.LongType },
    navArgument("arrowCount") { type = NavType.IntType },
)

private fun levelConfigArguments(): List<NamedNavArgument> = listOf(
    navArgument("levelId") { type = NavType.IntType },
    navArgument("gw") { type = NavType.IntType },
    navArgument("gh") { type = NavType.IntType },
    navArgument("arrows") { type = NavType.IntType },
    navArgument("maxFailures") { type = NavType.IntType },
    navArgument("timeLimit") { type = NavType.IntType },
    navArgument("complexity") { type = NavType.StringType },
    navArgument("obstacles") { type = NavType.IntType },
    navArgument("shape") { type = NavType.StringType },
    navArgument("shapeCells") { type = NavType.IntType },
    navArgument("perArrow") { type = NavType.IntType },
)
