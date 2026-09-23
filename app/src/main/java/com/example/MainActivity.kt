package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.entity.PlayerProfile
import com.example.model.AppScreen
import com.example.model.GameMode
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.ModeSelectScreen
import com.example.ui.screens.PrivateRoomScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LudoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: LudoViewModel = viewModel()
            val profile by viewModel.profileState.collectAsStateWithLifecycle()
            val isDarkTheme = profile?.darkThemeEnabled ?: false

            MyApplicationTheme(darkTheme = isDarkTheme) {
                LudoApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun LudoApp(viewModel: LudoViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val recentMatches by viewModel.recentMatches.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val bannerNotice by viewModel.bannerNotice.collectAsStateWithLifecycle()
    val roomCode by viewModel.roomCode.collectAsStateWithLifecycle()
    val roomPlayers by viewModel.roomPlayers.collectAsStateWithLifecycle()
    val isRoomHost by viewModel.isRoomHost.collectAsStateWithLifecycle()

    var pendingMode by remember { mutableStateOf(GameMode.VS_AI) }

    // System Back Handler
    BackHandler(enabled = currentScreen != AppScreen.SPLASH && currentScreen != AppScreen.HOME) {
        when (currentScreen) {
            AppScreen.GAME -> {
                // Return to home
                viewModel.navigateTo(AppScreen.HOME)
            }
            AppScreen.RESULT -> viewModel.navigateTo(AppScreen.HOME)
            AppScreen.MODE_SELECT -> viewModel.navigateTo(AppScreen.HOME)
            AppScreen.PRIVATE_ROOM -> viewModel.navigateTo(AppScreen.HOME)
            AppScreen.PROFILE, AppScreen.LEADERBOARD, AppScreen.SETTINGS -> viewModel.navigateTo(AppScreen.HOME)
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            AppScreen.SPLASH -> {
                SplashScreen(
                    onContinue = { viewModel.navigateTo(AppScreen.HOME) }
                )
            }
            AppScreen.HOME -> {
                HomeScreen(
                    profile = profile ?: PlayerProfile(),
                    onSelectMode = { mode ->
                        if (mode == GameMode.ONLINE_ROOM) {
                            viewModel.navigateTo(AppScreen.PRIVATE_ROOM)
                        } else {
                            pendingMode = mode
                            viewModel.navigateTo(AppScreen.MODE_SELECT)
                        }
                    },
                    onNavigate = { screen -> viewModel.navigateTo(screen) },
                    onClaimDailyReward = { viewModel.claimDailyReward() }
                )
            }
            AppScreen.MODE_SELECT -> {
                ModeSelectScreen(
                    gameMode = pendingMode,
                    userCoins = profile?.coins ?: 1500,
                    currentTheme = gameState.selectedTheme,
                    onBack = { viewModel.navigateTo(AppScreen.HOME) },
                    onStartGame = { playerCount, theme, stake ->
                        viewModel.startNewGame(
                            mode = pendingMode,
                            playerCount = playerCount,
                            selectedTheme = theme,
                            stake = stake
                        )
                    }
                )
            }
            AppScreen.PRIVATE_ROOM -> {
                PrivateRoomScreen(
                    roomCode = roomCode,
                    isHost = isRoomHost,
                    players = roomPlayers,
                    onCreateRoom = { count -> viewModel.createPrivateRoom(count, 200) },
                    onJoinRoom = { code -> viewModel.joinPrivateRoom(code) },
                    onStartGame = { viewModel.startRoomGameFromLobby() },
                    onBack = { viewModel.navigateTo(AppScreen.HOME) },
                    onCopyCodeNotice = { viewModel.setNotice("Room code copied to clipboard!") }
                )
            }
            AppScreen.GAME -> {
                GameScreen(
                    gameState = gameState,
                    isSoundMuted = viewModel.soundManager.isMuted,
                    onRollDice = { viewModel.rollDice() },
                    onTokenClick = { tokenId -> viewModel.onTokenSelected(tokenId) },
                    onToggleSound = { viewModel.toggleSound() },
                    onLeaveGame = { viewModel.navigateTo(AppScreen.HOME) }
                )
            }
            AppScreen.RESULT -> {
                ResultScreen(
                    gameState = gameState,
                    onPlayAgain = {
                        viewModel.startNewGame(
                            mode = gameState.gameMode,
                            playerCount = gameState.players.size,
                            selectedTheme = gameState.selectedTheme,
                            stake = gameState.entryStake
                        )
                    },
                    onGoHome = { viewModel.navigateTo(AppScreen.HOME) }
                )
            }
            AppScreen.PROFILE -> {
                ProfileScreen(
                    profile = profile ?: PlayerProfile(),
                    recentMatches = recentMatches,
                    achievements = achievements,
                    onUpdateProfile = { name, avatarId ->
                        viewModel.updateProfileNameAndAvatar(name, avatarId)
                    },
                    onClaimAchievement = { id -> viewModel.claimAchievement(id) },
                    onBack = { viewModel.navigateTo(AppScreen.HOME) }
                )
            }
            AppScreen.LEADERBOARD -> {
                LeaderboardScreen(
                    profile = profile ?: PlayerProfile(),
                    onBack = { viewModel.navigateTo(AppScreen.HOME) }
                )
            }
            AppScreen.SETTINGS -> {
                SettingsScreen(
                    profile = profile ?: PlayerProfile(),
                    currentTheme = gameState.selectedTheme,
                    onToggleSound = { viewModel.toggleSound() },
                    onToggleHaptics = { viewModel.toggleHaptics() },
                    onToggleDarkTheme = { viewModel.toggleDarkTheme() },
                    onSelectTheme = { theme -> viewModel.setBoardTheme(theme) },
                    onBack = { viewModel.navigateTo(AppScreen.HOME) }
                )
            }
        }

        // Global Banner Notification Snackbar
        AnimatedVisibility(
            visible = bannerNotice != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            bannerNotice?.let { notice ->
                Box(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF323232))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("banner_notice")
                ) {
                    Text(
                        text = notice,
                        color = Color(0xFFFFD54F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
