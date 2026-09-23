package com.example.engine

import com.example.model.GameState
import com.example.model.Player
import com.example.model.Token

object LudoAi {

    fun chooseTokenToMove(state: GameState, aiPlayer: Player, diceValue: Int, movableTokenIds: List<Int>): Int {
        if (movableTokenIds.isEmpty()) return -1
        if (movableTokenIds.size == 1) return movableTokenIds.first()

        var bestScore = Int.MIN_VALUE
        var chosenTokenId = movableTokenIds.first()

        for (tokenId in movableTokenIds) {
            val token = aiPlayer.tokens[tokenId]
            val score = evaluateMove(state, aiPlayer, token, diceValue)
            if (score > bestScore) {
                bestScore = score
                chosenTokenId = tokenId
            }
        }

        return chosenTokenId
    }

    private fun evaluateMove(state: GameState, aiPlayer: Player, token: Token, diceValue: Int): Int {
        var score = 0

        val newStep = if (token.inYard) 0 else token.step + diceValue

        // 1. Reaching home goal
        if (newStep == LudoGameEngine.GOAL_STEP) {
            score += 1000
        }

        // 2. Entering safe home stretch
        if (token.step < 51 && newStep >= 51) {
            score += 600
        }

        // 3. Releasing token from yard
        if (token.inYard && diceValue == 6) {
            score += 500
        }

        // 4. Capture opponent
        if (newStep in 0..50) {
            val landingOuter = (aiPlayer.color.startIndex + newStep) % 52
            val isSafe = LudoBoardCoordinates.isSafeCell(landingOuter)

            if (!isSafe) {
                for (otherPlayer in state.players) {
                    if (otherPlayer.id == aiPlayer.id) continue
                    for (otherToken in otherPlayer.tokens) {
                        val otherOuter = LudoBoardCoordinates.getOuterTrackIndex(otherToken)
                        if (otherOuter == landingOuter) {
                            score += 850 // High reward for capturing!
                        }
                    }
                }
            } else {
                // Landed on safe star
                score += 350
            }

            // 5. Check if landing square is threatened by an opponent token within 6 steps behind
            var isThreatened = false
            for (otherPlayer in state.players) {
                if (otherPlayer.id == aiPlayer.id) continue
                for (otherToken in otherPlayer.tokens) {
                    val otherOuter = LudoBoardCoordinates.getOuterTrackIndex(otherToken)
                    if (otherOuter != null) {
                        val distanceBehind = (landingOuter - otherOuter + 52) % 52
                        if (distanceBehind in 1..6) {
                            isThreatened = true
                        }
                    }
                }
            }
            if (!isSafe && isThreatened) {
                score -= 150
            }
        }

        // 6. Prefer moving token already furthest along
        score += newStep * 2

        return score
    }
}
