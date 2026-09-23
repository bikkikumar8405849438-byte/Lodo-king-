package com.example.model

enum class GameMode(val title: String, val description: String) {
    VS_AI("Play vs Computer", "Challenge smart AI bots anytime"),
    PASS_AND_PLAY("Pass & Play", "Play with friends on the same device"),
    ONLINE_ROOM("Private Room", "Create or join a private room with a code")
}
