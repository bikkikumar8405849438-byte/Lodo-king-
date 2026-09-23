package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameState
import com.example.model.PlayerColor
import com.example.ui.board.DiceComposable
import com.example.ui.board.LudoBoardComposable
import com.example.ui.components.PlayerCard

@Composable
fun GameScreen(
    gameState: GameState,
    isSoundMuted: Boolean,
    onRollDice: () -> Unit,
    onTokenClick: (Int) -> Unit,
    onToggleSound: () -> Unit,
    onLeaveGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLeaveDialog by remember { mutableStateOf(false) }
    val activePlayer = gameState.activePlayer

    Scaffold(
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                IconButton(
                    onClick = { showLeaveDialog = true },
                    modifier = Modifier.testTag("game_leave_btn")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Leave match")
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = gameState.gameMode.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier.testTag("game_mute_btn")
                ) {
                    Icon(
                        imageVector = if (isSoundMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Mute toggle",
                        tint = if (isSoundMuted) Color.Gray else MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            // 1. Other Players Header Cards (up to 3 opponents or 1 opponent in 2P)
            val otherPlayers = gameState.players.filter { it.id != activePlayer?.id }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                otherPlayers.forEach { p ->
                    PlayerCard(
                        player = p,
                        isActive = false,
                        secondsRemaining = 0,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Center 15x15 Interactive Ludo Board
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                LudoBoardComposable(
                    gameState = gameState,
                    onTokenClick = onTokenClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Active Turn Control Console
            if (activePlayer != null) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = activePlayer.color.primaryColor.copy(alpha = 0.09f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        activePlayer.color.primaryColor.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_player_console")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Player Identity & Turn Banner
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(activePlayer.color.primaryColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${activePlayer.name}'s Turn",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = activePlayer.color.primaryColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = gameState.statusMessage,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Interactive 3D Dice Composable
                            DiceComposable(
                                value = gameState.currentDiceValue,
                                isRolling = gameState.isRolling,
                                canRoll = gameState.canRollDice && !gameState.isRolling && (activePlayer.isAi || gameState.isUserTurn),
                                playerColor = activePlayer.color,
                                onRollClick = onRollDice
                            )
                        }

                        // Consecutive 6s tracker (if > 0)
                        if (gameState.consecutiveSixes > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🎲 Consecutive Sixes: ${gameState.consecutiveSixes}/3 (Roll 3x to lose turn)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Quit Match?") },
            text = { Text("Leaving the match now will forfeit your entry coins. Do you wish to return to the palace?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveDialog = false
                        onLeaveGame()
                    },
                    modifier = Modifier.testTag("confirm_leave_btn")
                ) {
                    Text("Quit Match")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Keep Playing")
                }
            }
        )
    }
}
