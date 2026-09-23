package com.example.engine

import androidx.compose.ui.geometry.Offset
import com.example.model.PlayerColor
import com.example.model.Token

data class BoardCoord(val row: Int, val col: Int)

object LudoBoardCoordinates {
    const val GRID_SIZE = 15

    // 52 outer track circular cells in clockwise order
    val outerTrackCells = listOf(
        BoardCoord(1, 6),   // 0: Green Start (Star)
        BoardCoord(2, 6),   // 1
        BoardCoord(3, 6),   // 2
        BoardCoord(4, 6),   // 3
        BoardCoord(5, 6),   // 4
        BoardCoord(6, 5),   // 5
        BoardCoord(6, 4),   // 6
        BoardCoord(6, 3),   // 7
        BoardCoord(6, 2),   // 8: Star Safe
        BoardCoord(6, 1),   // 9
        BoardCoord(6, 0),   // 10
        BoardCoord(7, 0),   // 11
        BoardCoord(8, 0),   // 12
        BoardCoord(8, 1),   // 13: Red Start (Star)
        BoardCoord(8, 2),   // 14
        BoardCoord(8, 3),   // 15
        BoardCoord(8, 4),   // 16
        BoardCoord(8, 5),   // 17
        BoardCoord(9, 6),   // 18
        BoardCoord(10, 6),  // 19
        BoardCoord(11, 6),  // 20
        BoardCoord(12, 6),  // 21: Star Safe
        BoardCoord(13, 6),  // 22
        BoardCoord(14, 6),  // 23
        BoardCoord(14, 7),  // 24
        BoardCoord(14, 8),  // 25
        BoardCoord(13, 8),  // 26: Blue Start (Star)
        BoardCoord(12, 8),  // 27
        BoardCoord(11, 8),  // 28
        BoardCoord(10, 8),  // 29
        BoardCoord(9, 8),   // 30
        BoardCoord(8, 9),   // 31
        BoardCoord(8, 10),  // 32
        BoardCoord(8, 11),  // 33
        BoardCoord(8, 12),  // 34: Star Safe
        BoardCoord(8, 13),  // 35
        BoardCoord(8, 14),  // 36
        BoardCoord(7, 14),  // 37
        BoardCoord(6, 14),  // 38
        BoardCoord(6, 13),  // 39: Yellow Start (Star)
        BoardCoord(6, 12),  // 40
        BoardCoord(6, 11),  // 41
        BoardCoord(6, 10),  // 42
        BoardCoord(6, 9),   // 43
        BoardCoord(5, 8),   // 44
        BoardCoord(4, 8),   // 45
        BoardCoord(3, 8),   // 46
        BoardCoord(2, 8),   // 47: Star Safe
        BoardCoord(1, 8),   // 48
        BoardCoord(0, 8),   // 49
        BoardCoord(0, 7),   // 50
        BoardCoord(0, 6)    // 51
    )

    // The 8 classic Star safe cells on the outer track
    val safeCellOuterIndices = setOf(0, 8, 13, 21, 26, 34, 39, 47)

    fun isSafeCell(outerIndex: Int): Boolean = outerIndex in safeCellOuterIndices

    // Home straight paths (Steps 51 to 55) and Center Goal (Step 56)
    val greenHomePath = listOf(
        BoardCoord(1, 7), BoardCoord(2, 7), BoardCoord(3, 7), BoardCoord(4, 7), BoardCoord(5, 7),
        BoardCoord(6, 7) // Goal
    )

    val redHomePath = listOf(
        BoardCoord(7, 1), BoardCoord(7, 2), BoardCoord(7, 3), BoardCoord(7, 4), BoardCoord(7, 5),
        BoardCoord(7, 6) // Goal
    )

    val blueHomePath = listOf(
        BoardCoord(13, 7), BoardCoord(12, 7), BoardCoord(11, 7), BoardCoord(10, 7), BoardCoord(9, 7),
        BoardCoord(8, 7) // Goal
    )

    val yellowHomePath = listOf(
        BoardCoord(7, 13), BoardCoord(7, 12), BoardCoord(7, 11), BoardCoord(7, 10), BoardCoord(7, 9),
        BoardCoord(7, 8) // Goal
    )

    // Yard token centers (floating row, col)
    fun getYardPosition(color: PlayerColor, tokenId: Int): Offset {
        return when (color) {
            PlayerColor.GREEN -> when (tokenId) {
                0 -> Offset(1.5f, 1.5f)
                1 -> Offset(1.5f, 3.5f)
                2 -> Offset(3.5f, 1.5f)
                else -> Offset(3.5f, 3.5f)
            }
            PlayerColor.RED -> when (tokenId) {
                0 -> Offset(10.5f, 1.5f)
                1 -> Offset(10.5f, 3.5f)
                2 -> Offset(12.5f, 1.5f)
                else -> Offset(12.5f, 3.5f)
            }
            PlayerColor.BLUE -> when (tokenId) {
                0 -> Offset(10.5f, 10.5f)
                1 -> Offset(10.5f, 12.5f)
                2 -> Offset(12.5f, 10.5f)
                else -> Offset(12.5f, 12.5f)
            }
            PlayerColor.YELLOW -> when (tokenId) {
                0 -> Offset(1.5f, 10.5f)
                1 -> Offset(1.5f, 12.5f)
                2 -> Offset(3.5f, 10.5f)
                else -> Offset(3.5f, 12.5f)
            }
        }
    }

    /**
     * Converts a token's step to board coordinates (row, col).
     */
    fun getTokenBoardPosition(token: Token): Offset {
        if (token.inYard) {
            return getYardPosition(token.color, token.id)
        }

        if (token.onOuterTrack) {
            val outerIdx = (token.color.startIndex + token.step) % 52
            val cell = outerTrackCells[outerIdx]
            return Offset(cell.row.toFloat(), cell.col.toFloat())
        }

        if (token.step in 51..56) {
            val homeIdx = token.step - 51
            val path = when (token.color) {
                PlayerColor.GREEN -> greenHomePath
                PlayerColor.RED -> redHomePath
                PlayerColor.BLUE -> blueHomePath
                PlayerColor.YELLOW -> yellowHomePath
            }
            val cell = path[homeIdx.coerceIn(0, path.size - 1)]
            return Offset(cell.row.toFloat(), cell.col.toFloat())
        }

        return getYardPosition(token.color, token.id)
    }

    /**
     * Gets the outer track index for a token if it's currently on the outer track, else null.
     */
    fun getOuterTrackIndex(token: Token): Int? {
        if (!token.onOuterTrack) return null
        return (token.color.startIndex + token.step) % 52
    }
}
