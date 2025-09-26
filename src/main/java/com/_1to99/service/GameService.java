package com._1to99.service;

import com._1to99.exception.GameException;
import com._1to99.model.GameRoom;
import com._1to99.model.GameTurn;

public interface GameService {

    GameRoom createRoom(String playerName);

    GameRoom joinRoom(String roomId, String playerName) throws GameException;

    GameRoom startGame(String roomId, String playerId) throws GameException;

    GameTurn makeGuess(String roomId, String playerId, int guess) throws GameException;

    GameRoom getRoom(String roomId);

    GameRoom playerQuitGame(String roomId, String playerName) throws GameException;

    GameRoom restartGame(String roomId, String hostId) throws GameException;

    GameRoom removePlayer(String roomId, String hostId, String playerName) throws GameException;

}
