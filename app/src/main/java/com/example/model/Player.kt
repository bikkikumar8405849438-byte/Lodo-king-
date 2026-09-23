package com.example.model

data class Player(
    val id: String,
    val name: String,
    val color: PlayerColor,
    val avatarId: Int = 0,
    val isAi: Boolean = false,
    val isConnected: Boolean = true,
    val tokens: List<Token> = List(4) { idx -> Token(id = idx, color = color) },
    val hasFinished: Boolean = false,
    val rank: Int? = null
) {
    val tokensInHome: Int get() = tokens.count { it.isHome }
    val tokensInYard: Int get() = tokens.count { it.inYard }
    val tokensActive: Int get() = tokens.count { !it.inYard && !it.isHome }
}
