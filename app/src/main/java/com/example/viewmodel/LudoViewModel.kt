package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.data.db.LudoDatabase
import com.example.data.entity.GameAchievement
import com.example.data.entity.MatchHistory
import com.example.data.entity.PlayerProfile
import com.example.data.repository.LudoRepository
import com.example.engine.LudoAi
import com.example.engine.LudoGameEngine
import com.example.engine.MoveResult
import com.example.model.AppScreen
import com.example.model.BoardTheme
import com.example.model.GameMode
import com.example.model.GameState
import com.example.model.Player
import com.example.model.PlayerColor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class LudoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LudoRepository
    val soundManager = SoundManager()

    val profileState: StateFlow<PlayerProfile?>
    val recentMatches: StateFlow<List<MatchHistory>>
    val achievements: StateFlow<List<GameAchievement>>

    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _bannerNotice = MutableStateFlow<String?>(null)
    val bannerNotice: StateFlow<String?> = _bannerNotice.asStateFlow()

    // Private Room lobby state
    private val _roomPlayers = MutableStateFlow<List<Player>>(emptyList())
    val roomPlayers: StateFlow<List<Player>> = _roomPlayers.asStateFlow()

    private val _roomCode = MutableStateFlow<String?>(null)
    val roomCode: StateFlow<String?> = _roomCode.asStateFlow()

    private val _isRoomHost = MutableStateFlow(true)
    val isRoomHost: StateFlow<Boolean> = _isRoomHost.asStateFlow()

    private var aiTurnJob: Job? = null
    private var timerJob: Job? = null

    init {
        val db = LudoDatabase.getDatabase(application)
        repository = LudoRepository(db.ludoDao())

        profileState = repository.playerProfile.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerProfile()
        )

        recentMatches = repository.recentMatches.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        achievements = repository.achievements.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            repository.initializeDefaultsIfEmpty()
            // Sync sound mute setting from profile
            profileState.collect { profile ->
                profile?.let {
                    soundManager.isMuted = !it.soundEnabled
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        soundManager.playButtonClick()
        _currentScreen.value = screen
    }

    fun setNotice(msg: String) {
        _bannerNotice.value = msg
        viewModelScope.launch {
            delay(3000)
            if (_bannerNotice.value == msg) {
                _bannerNotice.value = null
            }
        }
    }

    /**
     * Initializes and starts a new game.
     */
    fun startNewGame(
        mode: GameMode,
        playerCount: Int,
        selectedTheme: BoardTheme = gameState.value.selectedTheme,
        stake: Int = 100
    ) {
        aiTurnJob?.cancel()
        timerJob?.cancel()

        val profile = profileState.value ?: PlayerProfile()
        val players = createPlayersForGame(mode, playerCount, profile)

        _gameState.value = GameState(
            gameMode = mode,
            players = players,
            activePlayerIndex = 0,
            currentDiceValue = null,
            isRolling = false,
            canRollDice = true,
            movableTokenIds = emptyList(),
            consecutiveSixes = 0,
            statusMessage = "${players.first().name}'s turn. Roll the dice!",
            winners = emptyList(),
            isGameOver = false,
            selectedTheme = selectedTheme,
            entryStake = stake,
            turnSecondsRemaining = 20
        )

        _currentScreen.value = AppScreen.GAME
        startTurnTimer()

        // If first player is AI (e.g. in certain modes), trigger it
        checkAndTriggerAi()
    }

    private fun createPlayersForGame(mode: GameMode, playerCount: Int, profile: PlayerProfile): List<Player> {
        val colors = if (playerCount == 2) {
            listOf(PlayerColor.RED, PlayerColor.YELLOW)
        } else {
            listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW, PlayerColor.BLUE)
        }

        return colors.mapIndexed { index, color ->
            when (mode) {
                GameMode.VS_AI -> {
                    if (index == 0) {
                        Player(
                            id = "user",
                            name = profile.name.ifBlank { "Player 1" },
                            color = color,
                            avatarId = profile.avatarId,
                            isAi = false
                        )
                    } else {
                        val botNames = listOf("Alex Bot", "Maya Bot", "Rohan Bot", "Leo Bot")
                        Player(
                            id = "bot_$index",
                            name = botNames.getOrElse(index - 1) { "Bot $index" },
                            color = color,
                            avatarId = (index + 2) % 6,
                            isAi = true
                        )
                    }
                }
                GameMode.PASS_AND_PLAY -> {
                    Player(
                        id = "local_$index",
                        name = if (index == 0) profile.name.ifBlank { "Player 1" } else "Player ${index + 1}",
                        color = color,
                        avatarId = index % 6,
                        isAi = false
                    )
                }
                GameMode.ONLINE_ROOM -> {
                    if (index == 0) {
                        Player(
                            id = "user",
                            name = profile.name.ifBlank { "Host" },
                            color = color,
                            avatarId = profile.avatarId,
                            isAi = false
                        )
                    } else {
                        val guests = listOf("Vikram", "Priya", "Anand")
                        Player(
                            id = "online_player_$index",
                            name = guests.getOrElse(index - 1) { "Player ${index + 1}" },
                            color = color,
                            avatarId = (index + 1) % 6,
                            isAi = false,
                            isConnected = true
                        )
                    }
                }
            }
        }
    }

    /**
     * User or AI initiates a dice roll.
     */
    fun rollDice() {
        val state = _gameState.value
        if (!state.canRollDice || state.isRolling || state.isGameOver) return

        val active = state.activePlayer ?: return

        // Audio & Visual roll initiation
        soundManager.playDiceRoll()
        _gameState.value = state.copy(isRolling = true, canRollDice = false)

        viewModelScope.launch {
            // Animate rolling effect for 600ms
            delay(550)
            val rolledValue = Random.nextInt(1, 7)
            val isSix = (rolledValue == 6)

            if (isSix) {
                soundManager.playSixRolled()
            }

            val nextConsecutiveSixes = if (isSix) state.consecutiveSixes + 1 else 0
            val isTripleSix = nextConsecutiveSixes >= LudoGameEngine.MAX_CONSECUTIVE_SIXES

            var userSixesInc = 0
            if (active.id == "user" && isSix) {
                userSixesInc = 1
            }

            if (isTripleSix) {
                // Triple 6 penalty! Turn lost!
                val passed = LudoGameEngine.passTurn(state, "Three consecutive 6s! Turn forfeited.")
                _gameState.value = passed.copy(
                    isRolling = false,
                    userSixesCount = state.userSixesCount + userSixesInc
                )
                startTurnTimer()
                checkAndTriggerAi()
                return@launch
            }

            val movableIds = LudoGameEngine.getMovableTokenIds(active, rolledValue)

            val status = if (movableIds.isEmpty()) {
                "${active.name} rolled $rolledValue. No valid moves."
            } else {
                "${active.name} rolled $rolledValue! Tap a glowing token to move."
            }

            _gameState.value = state.copy(
                currentDiceValue = rolledValue,
                isRolling = false,
                canRollDice = false,
                movableTokenIds = movableIds,
                consecutiveSixes = nextConsecutiveSixes,
                statusMessage = status,
                userSixesCount = state.userSixesCount + userSixesInc
            )

            if (movableIds.isEmpty()) {
                // No movable tokens: wait 1.2s and pass turn
                delay(1200)
                val passed = LudoGameEngine.passTurn(_gameState.value, "No moves possible.")
                _gameState.value = passed
                startTurnTimer()
                checkAndTriggerAi()
            } else if (active.isAi) {
                // Let AI pick the best token after short realistic deliberation
                delay(700)
                val chosenTokenId = LudoAi.chooseTokenToMove(_gameState.value, active, rolledValue, movableIds)
                onTokenSelected(chosenTokenId)
            } else if (movableIds.size == 1 && active.tokens.count { !it.isHome } == 1) {
                // Auto move single remaining active token for snappy UX
                delay(300)
                onTokenSelected(movableIds.first())
            }
        }
    }

    /**
     * Called when a player taps on a token to move it.
     */
    fun onTokenSelected(tokenId: Int) {
        val state = _gameState.value
        val active = state.activePlayer ?: return
        if (state.isRolling || state.isGameOver) return
        if (state.currentDiceValue == null) return
        if (tokenId !in state.movableTokenIds) return

        soundManager.playTokenMove()

        val result = LudoGameEngine.applyMove(state, tokenId)
        if (result is MoveResult.Success) {
            val updated = result.updatedState
            _gameState.value = updated

            if (result.capturedColor != null) {
                soundManager.playCapture()
            }
            if (result.reachedHome) {
                soundManager.playTokenHome()
            }

            if (result.wonGame || updated.isGameOver) {
                soundManager.playVictory()
                onGameOver(updated)
                return
            }

            startTurnTimer()
            checkAndTriggerAi()
        }
    }

    private fun checkAndTriggerAi() {
        val state = _gameState.value
        if (state.isGameOver) return

        val active = state.activePlayer ?: return
        if (active.isAi) {
            aiTurnJob?.cancel()
            aiTurnJob = viewModelScope.launch {
                delay(800)
                rollDice()
            }
        }
    }

    private fun startTurnTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var timeLeft = 20
            while (timeLeft > 0) {
                _gameState.value = _gameState.value.copy(turnSecondsRemaining = timeLeft)
                delay(1000)
                timeLeft--
            }
            // Auto timeout: if user didn't roll or move in 20s, auto pass or auto roll
            val current = _gameState.value
            if (!current.isGameOver) {
                if (current.canRollDice) {
                    rollDice()
                } else if (current.movableTokenIds.isNotEmpty()) {
                    onTokenSelected(current.movableTokenIds.first())
                }
            }
        }
    }

    private fun onGameOver(state: GameState) {
        timerJob?.cancel()
        aiTurnJob?.cancel()

        val profile = profileState.value ?: PlayerProfile()
        val userWon = state.winners.firstOrNull()?.id == "user"
        val winnerName = state.winners.firstOrNull()?.name ?: "Winner"

        val prizePool = state.entryStake * state.players.size
        val coinsDelta = if (userWon) prizePool - state.entryStake else -state.entryStake

        viewModelScope.launch {
            repository.recordMatch(
                mode = state.gameMode.name,
                playerCount = state.players.size,
                userColor = state.players.firstOrNull { it.id == "user" }?.color?.name ?: "RED",
                resultWon = userWon,
                winnerName = winnerName,
                coinsDelta = coinsDelta,
                turnsCount = state.turnsCount,
                tokensCapturedInMatch = state.userCapturedCount,
                sixesRolledInMatch = state.userSixesCount
            )
        }

        _currentScreen.value = AppScreen.RESULT
    }

    // --- Private Room Management ---

    fun createPrivateRoom(playerCount: Int, stake: Int) {
        val randomNum = Random.nextInt(1000, 9999)
        val code = "LUDO-$randomNum"
        _roomCode.value = code
        _isRoomHost.value = true

        val profile = profileState.value ?: PlayerProfile()
        val host = Player(
            id = "user",
            name = profile.name.ifBlank { "Host" },
            color = PlayerColor.RED,
            avatarId = profile.avatarId,
            isAi = false
        )
        val guests = (1 until playerCount).map { idx ->
            val color = when (idx) {
                1 -> if (playerCount == 2) PlayerColor.YELLOW else PlayerColor.GREEN
                2 -> PlayerColor.YELLOW
                else -> PlayerColor.BLUE
            }
            Player(
                id = "guest_$idx",
                name = "Guest $idx",
                color = color,
                avatarId = idx,
                isAi = false,
                isConnected = true
            )
        }

        _roomPlayers.value = listOf(host) + guests
        _currentScreen.value = AppScreen.PRIVATE_ROOM
    }

    fun joinPrivateRoom(code: String) {
        if (code.isBlank() || code.length < 4) {
            setNotice("Please enter a valid room code")
            return
        }
        _roomCode.value = code.uppercase()
        _isRoomHost.value = false

        val profile = profileState.value ?: PlayerProfile()
        val user = Player(
            id = "user",
            name = profile.name.ifBlank { "Player" },
            color = PlayerColor.YELLOW,
            avatarId = profile.avatarId,
            isAi = false
        )
        val host = Player(
            id = "host",
            name = "Room Host",
            color = PlayerColor.RED,
            avatarId = 1,
            isAi = false
        )

        _roomPlayers.value = listOf(host, user)
        _currentScreen.value = AppScreen.PRIVATE_ROOM
        setNotice("Joined room $code successfully!")
    }

    fun startRoomGameFromLobby() {
        val players = _roomPlayers.value
        val count = players.size
        startNewGame(
            mode = GameMode.ONLINE_ROOM,
            playerCount = count,
            selectedTheme = _gameState.value.selectedTheme,
            stake = 200
        )
    }

    // --- Profile & Rewards & Settings ---

    fun claimDailyReward() {
        viewModelScope.launch {
            val reward = repository.claimDailyReward()
            if (reward > 0) {
                soundManager.playVictory()
                setNotice("Claimed $reward Gold Coins! Streak updated.")
            } else {
                setNotice("Daily reward already claimed today. Check back tomorrow!")
            }
        }
    }

    fun claimAchievement(id: String) {
        viewModelScope.launch {
            val reward = repository.claimAchievement(id)
            if (reward > 0) {
                soundManager.playSixRolled()
                setNotice("Achievement claimed! +$reward Gold Coins")
            }
        }
    }

    fun updateProfileNameAndAvatar(name: String, avatarId: Int) {
        viewModelScope.launch {
            val current = profileState.value ?: return@launch
            repository.updateProfile(current.copy(name = name.trim(), avatarId = avatarId))
            setNotice("Profile updated successfully")
        }
    }

    fun setBoardTheme(theme: BoardTheme) {
        _gameState.value = _gameState.value.copy(selectedTheme = theme)
        viewModelScope.launch {
            val current = profileState.value ?: return@launch
            repository.updateProfile(current.copy(preferredTheme = theme.name))
        }
    }

    fun toggleSound() {
        viewModelScope.launch {
            val current = profileState.value ?: return@launch
            val next = !current.soundEnabled
            soundManager.isMuted = !next
            repository.updateProfile(current.copy(soundEnabled = next))
        }
    }

    fun toggleHaptics() {
        viewModelScope.launch {
            val current = profileState.value ?: return@launch
            repository.updateProfile(current.copy(hapticsEnabled = !current.hapticsEnabled))
        }
    }

    fun toggleDarkTheme() {
        viewModelScope.launch {
            val current = profileState.value ?: return@launch
            repository.updateProfile(current.copy(darkThemeEnabled = !current.darkThemeEnabled))
        }
    }
}
