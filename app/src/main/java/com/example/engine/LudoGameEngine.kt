package com.example.engine

import com.example.model.GameState
import com.example.model.Player
import com.example.model.PlayerColor
import com.example.model.Token

sealed class MoveResult {
    data class Success(
        val updatedState: GameState,
        val capturedColor: PlayerColor? = null,
        val reachedHome: Boolean = false,
        val hasBonusRoll: Boolean = false,
        val wonGame: Boolean = false
    ) : MoveResult()
    data class Invalid(val reason: String) : MoveResult()
}

object LudoGameEngine {

    const val GOAL_STEP = 56
    const val MAX_CONSECUTIVE_SIXES = 3

    fun canTokenMove(token: Token, diceValue: Int): Boolean {
        if (token.isHome) return false
        if (token.inYard) {
            return diceValue == 6
        }
        return (token.step + diceValue) <= GOAL_STEP
    }

    fun getMovableTokenIds(player: Player, diceValue: Int): List<Int> {
        if (player.hasFinished) return emptyList()
        return player.tokens
            .filter { canTokenMove(it, diceValue) }
            .map { it.id }
    }

    /**
     * Executes the token move, handles captures, home completions, extra turns, and turn switching.
     */
    fun applyMove(state: GameState, tokenId: Int): MoveResult {
        val player = state.activePlayer ?: return MoveResult.Invalid("No active player")
        val dice = state.currentDiceValue ?: return MoveResult.Invalid("No dice roll")
        val token = player.tokens.getOrNull(tokenId) ?: return MoveResult.Invalid("Token not found")

        if (!canTokenMove(token, dice)) {
            return MoveResult.Invalid("Token cannot move with dice roll $dice")
        }

        // Calculate new step
        val newStep = if (token.inYard) 0 else token.step + dice
        val reachedHome = (newStep == GOAL_STEP)
        val updatedToken = token.copy(step = newStep)
        val updatedTokens = player.tokens.map { if (it.id == tokenId) updatedToken else it }

        var capturedPlayerColor: PlayerColor? = null
        var userCapturedInc = 0

        // Check for captures on outer track
        val mutablePlayers = state.players.toMutableList()
        val currentPlayerIndex = state.activePlayerIndex

        if (newStep in 0..50) {
            val landingOuterIdx = (player.color.startIndex + newStep) % 52
            val isSafe = LudoBoardCoordinates.isSafeCell(landingOuterIdx)

            if (!isSafe) {
                // Opponent tokens on this cell get sent to yard
                for (i in mutablePlayers.indices) {
                    if (i == currentPlayerIndex) continue
                    val otherPlayer = mutablePlayers[i]
                    var hadCapture = false
                    val modifiedTokens = otherPlayer.tokens.map { otherToken ->
                        val otherOuterIdx = LudoBoardCoordinates.getOuterTrackIndex(otherToken)
                        if (otherOuterIdx == landingOuterIdx) {
                            hadCapture = true
                            otherToken.copy(step = -1) // Return to base yard!
                        } else {
                            otherToken
                        }
                    }
                    if (hadCapture) {
                        capturedPlayerColor = otherPlayer.color
                        mutablePlayers[i] = otherPlayer.copy(tokens = modifiedTokens)
                        if (player.id == "user") {
                            userCapturedInc++
                        }
                    }
                }
            }
        }

        // Check if player has finished (all 4 tokens in home)
        val allFinished = updatedTokens.all { it.isHome }
        val updatedCurrentPlayer = player.copy(
            tokens = updatedTokens,
            hasFinished = allFinished,
            rank = if (allFinished) (state.winners.size + 1) else player.rank
        )
        mutablePlayers[currentPlayerIndex] = updatedCurrentPlayer

        val newWinners = if (allFinished && !state.winners.any { it.id == player.id }) {
            state.winners + updatedCurrentPlayer
        } else {
            state.winners
        }

        // Determine if game is over
        // For 2 players: ends when 1 wins. For 4 players: ends when 1 remains or 3 finish
        val remainingPlayers = mutablePlayers.filter { !it.hasFinished }
        val isGameOver = remainingPlayers.size <= 1 || (state.players.size == 2 && newWinners.isNotEmpty())

        // Bonus roll conditions:
        // 1. Rolled a 6 (unless consecutive 6s reached 3)
        // 2. Captured an opponent token
        // 3. Token reached home
        val rolledSix = (dice == 6)
        val hasBonusRoll = !allFinished && !isGameOver && (
            (rolledSix && state.consecutiveSixes < MAX_CONSECUTIVE_SIXES) ||
            capturedPlayerColor != null ||
            reachedHome
        )

        val nextPlayerIndex = if (hasBonusRoll) {
            currentPlayerIndex
        } else {
            getNextActivePlayerIndex(mutablePlayers, currentPlayerIndex)
        }

        val nextConsecutiveSixes = if (hasBonusRoll && rolledSix) {
            state.consecutiveSixes
        } else {
            0
        }

        val statusMsg = buildString {
            append("${player.name} moved token")
            if (capturedPlayerColor != null) {
                append(" and captured ${capturedPlayerColor.title}'s token! 💥")
            } else if (reachedHome) {
                append(" safely HOME! 🌟")
            }
            if (hasBonusRoll) {
                append(" Bonus roll granted!")
            }
        }

        val updatedState = state.copy(
            players = mutablePlayers,
            activePlayerIndex = nextPlayerIndex,
            currentDiceValue = null,
            canRollDice = true,
            movableTokenIds = emptyList(),
            consecutiveSixes = nextConsecutiveSixes,
            winners = newWinners,
            isGameOver = isGameOver,
            statusMessage = statusMsg,
            turnSecondsRemaining = 20,
            turnsCount = state.turnsCount + 1,
            userCapturedCount = state.userCapturedCount + userCapturedInc,
            animatingTokenColor = player.color,
            animatingTokenId = tokenId
        )

        return MoveResult.Success(
            updatedState = updatedState,
            capturedColor = capturedPlayerColor,
            reachedHome = reachedHome,
            hasBonusRoll = hasBonusRoll,
            wonGame = allFinished
        )
    }

    /**
     * Pass turn to next active player if no valid moves exist or after triple six.
     */
    fun passTurn(state: GameState, reason: String): GameState {
        val nextIndex = getNextActivePlayerIndex(state.players, state.activePlayerIndex)
        val nextPlayer = state.players.getOrNull(nextIndex)
        return state.copy(
            activePlayerIndex = nextIndex,
            currentDiceValue = null,
            canRollDice = true,
            movableTokenIds = emptyList(),
            consecutiveSixes = 0,
            turnSecondsRemaining = 20,
            statusMessage = "$reason Next turn: ${nextPlayer?.name ?: "Player"}"
        )
    }

    private fun getNextActivePlayerIndex(players: List<Player>, currentIndex: Int): Int {
        if (players.isEmpty()) return 0
        var next = (currentIndex + 1) % players.size
        var checks = 0
        while (checks < players.size) {
            if (!players[next].hasFinished) {
                return next
            }
            next = (next + 1) % players.size
            checks++
        }
        return currentIndex
    }
}
