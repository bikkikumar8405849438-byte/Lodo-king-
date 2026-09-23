package com.example.model

data class GameState(
    val gameMode: GameMode = GameMode.VS_AI,
    val players: List<Player> = emptyList(),
    val activePlayerIndex: Int = 0,
    val currentDiceValue: Int? = null,
    val isRolling: Boolean = false,
    val canRollDice: Boolean = true,
    val movableTokenIds: List<Int> = emptyList(),
    val consecutiveSixes: Int = 0,
    val statusMessage: String = "Roll the dice to begin!",
    val winners: List<Player> = emptyList(),
    val isGameOver: Boolean = false,
    val selectedTheme: BoardTheme = BoardTheme.ROYAL_CLASSIC,
    val entryStake: Int = 100,
    val roomCode: String? = null,
    val isHost: Boolean = true,
    val turnSecondsRemaining: Int = 20,
    val turnsCount: Int = 0,
    val userCapturedCount: Int = 0,
    val userSixesCount: Int = 0,
    val animatingTokenColor: PlayerColor? = null,
    val animatingTokenId: Int? = null
) {
    val activePlayer: Player?
        get() = if (players.isNotEmpty() && activePlayerIndex in players.indices) players[activePlayerIndex] else null

    val isUserTurn: Boolean
        get() = activePlayer?.let { !it.isAi && (gameMode == GameMode.PASS_AND_PLAY || it.id == "user") } ?: false
}
