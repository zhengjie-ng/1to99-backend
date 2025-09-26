package com._1to99.dto;

import com._1to99.model.GameRoom;
import com._1to99.model.GameTurn;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameUpdateMessage {
    private String type; // "ROOM_CREATED", "PLAYER_JOINED", "GAME_STARTED", etc.
    private GameRoom gameRoom;
    private String message;
    private GameTurn lastTurn;
    private Boolean allPlayAgain; // For ALL_PLAYERS_DECIDED messages
}
