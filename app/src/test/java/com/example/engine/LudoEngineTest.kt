package com.example.engine

import com.example.model.GameMode
import com.example.model.GameState
import com.example.model.Player
import com.example.model.PlayerColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoEngineTest {

    private fun createInitialState(): GameState {
        val p1 = Player(id = "p1", name = "Red Player", color = PlayerColor.RED)
        val p2 = Player(id = "p2", name = "Green Player", color = PlayerColor.GREEN)
        return GameState(
            gameMode = GameMode.PASS_AND_PLAY,
            players = listOf(p1, p2),
            activePlayerIndex = 0,
            canRollDice = true
        )
    }

    @Test
    fun testYardTokenCannotMoveOnNonSix() {
        val state = createInitialState()
        val movableOnFive = LudoGameEngine.getMovableTokenIds(state.players[0], 5)
        assertTrue("Tokens in yard should not move on roll 5", movableOnFive.isEmpty())

        val movableOnSix = LudoGameEngine.getMovableTokenIds(state.players[0], 6)
        assertEquals("All 4 yard tokens should be movable on roll 6", 4, movableOnSix.size)
    }

    @Test
    fun testTokenReleasesOnSix() {
        var state = createInitialState()
        state = state.copy(currentDiceValue = 6, movableTokenIds = listOf(0))

        val result = LudoGameEngine.applyMove(state, 0)
        assertTrue(result is MoveResult.Success)
        val updated = (result as MoveResult.Success).updatedState

        val active = updated.players[0]
        val token0 = active.tokens[0]
        assertFalse("Token should be out of yard", token0.inYard)
        assertEquals("Token step should be 0", 0, token0.step)
        assertTrue("Player should get bonus turn on rolling a 6", updated.canRollDice)
        assertEquals("Active player should remain same on bonus turn", 0, updated.activePlayerIndex)
    }

    @Test
    fun testSafeCellPreventsCapture() {
        // Star cell 0 is safe cell
        assertTrue("Outer track 0 should be safe", LudoBoardCoordinates.isSafeCell(0))
        assertTrue("Outer track 8 should be safe", LudoBoardCoordinates.isSafeCell(8))
        assertFalse("Outer track 1 should not be safe", LudoBoardCoordinates.isSafeCell(1))
    }

    @Test
    fun testVictoryWhenAllFourTokensReachHome() {
        var state = createInitialState()
        val p1Tokens = state.players[0].tokens.mapIndexed { idx, t ->
            if (idx == 0) t.copy(step = 55)
            else t.copy(step = 56)
        }
        val p1 = state.players[0].copy(tokens = p1Tokens)
        state = state.copy(
            players = listOf(p1, state.players[1]),
            currentDiceValue = 1,
            movableTokenIds = listOf(0)
        )

        val result = LudoGameEngine.applyMove(state, 0)
        assertTrue(result is MoveResult.Success)
        val success = result as MoveResult.Success
        assertTrue("Game should be won when all tokens reach home", success.wonGame)
        assertTrue("Game state should be over", success.updatedState.isGameOver)
        assertEquals("Winner should be p1", "p1", success.updatedState.winners.first().id)
    }
}
