package com._1to99.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com._1to99.exception.GameException;
import com._1to99.model.GameRoom;
import com._1to99.model.GameState;
import com._1to99.model.GameTurn;
import com._1to99.model.Player;
import com._1to99.service.GameService;

@Service
public class GameServiceImpl implements GameService {
    
private final Map<String, GameRoom> gameRooms = new ConcurrentHashMap<>();
    private final Map<String, String> playerRoomMap = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public GameRoom createRoom(String hostName) {
        String roomId = generateRoomId();
        String hostId = UUID.randomUUID().toString();
        
        Player host = new Player(hostId, hostName, true);
        GameRoom room = new GameRoom();
        room.setRoomId(roomId);
        room.setHostId(hostId);
        room.setPlayers(new ArrayList<>(Arrays.asList(host)));
        room.setState(GameState.WAITING_FOR_PLAYERS);
        
        gameRooms.put(roomId, room);
        playerRoomMap.put(hostId, roomId);
        
        return room;
    }

    public GameRoom joinRoom(String roomId, String playerName) throws GameException {
        GameRoom room = gameRooms.get(roomId);
        if (room == null) {
            throw new GameException("Room not found");
        }
        if (room.getState() != GameState.WAITING_FOR_PLAYERS) {
            throw new GameException("Game already started");
        }
        
        String playerId = UUID.randomUUID().toString();
        Player player = new Player(playerId, playerName, false);
        room.getPlayers().add(player);
        playerRoomMap.put(playerId, roomId);
        
        return room;
    }

    public GameRoom startGame(String roomId, String hostId) throws GameException {
        GameRoom room = gameRooms.get(roomId);
        if (room == null || !room.getHostId().equals(hostId)) {
            throw new GameException("Unauthorized or room not found");
        }

        // Auto-shuffle player order before starting the game
        shufflePlayersForGameStart(room);

        room.setSecretNumber(random.nextInt(99) + 1);
        room.setMinRange(1);
        room.setMaxRange(99);
        room.setCurrentPlayerIndex(0);
        room.setState(GameState.IN_PROGRESS);
        room.setGameHistory(new ArrayList<>());

        return room;
    }

    private void shufflePlayersForGameStart(GameRoom room) {
        if (room.getPlayers().size() > 1) {
            // Create a copy of the players list for shuffling
            ArrayList<Player> allPlayers = new ArrayList<>(room.getPlayers());

            // Shuffle all players (including host) for fair turn order
            for (int i = allPlayers.size() - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                Player temp = allPlayers.get(i);
                allPlayers.set(i, allPlayers.get(j));
                allPlayers.set(j, temp);
            }

            // Update the room with shuffled player order
            room.setPlayers(allPlayers);
        }
    }

    public GameTurn makeGuess(String roomId, String playerId, int guess) throws GameException {
        GameRoom room = gameRooms.get(roomId);
        if (room == null || room.getState() != GameState.IN_PROGRESS) {
            throw new GameException("Invalid game state");
        }
        
        Player currentPlayer = room.getPlayers().get(room.getCurrentPlayerIndex());
        if (!currentPlayer.getId().equals(playerId)) {
            throw new GameException("Not your turn");
        }
        
        GameTurn turn = new GameTurn();
        turn.setPlayerId(playerId);
        turn.setPlayerName(currentPlayer.getName());
        turn.setGuess(guess);
        turn.setTimestamp(System.currentTimeMillis());
        
        if (guess == room.getSecretNumber()) {
            turn.setResult("SECRET NUMBER GUESSED! " + currentPlayer.getName() + " lost!");
            room.setState(GameState.FINISHED);
        } else if (guess < room.getSecretNumber()) {
            room.setMinRange(guess + 1);
            turn.setResult("Range: " + room.getMinRange() + "-" + room.getMaxRange());
            moveToNextPlayer(room);
        } else {
            room.setMaxRange(guess - 1);
            turn.setResult("Range: " + room.getMinRange() + "-" + room.getMaxRange());
            moveToNextPlayer(room);
        }
        
        room.getGameHistory().add(turn);
        return turn;
    }

    private void moveToNextPlayer(GameRoom room) {
        int nextIndex = (room.getCurrentPlayerIndex() + 1) % room.getPlayers().size();
        room.setCurrentPlayerIndex(nextIndex);
    }

    private String generateRoomId() {
        return String.valueOf(random.nextInt(9000) + 1000);
    }

    @Override
    public GameRoom getRoom(String roomId) {
        return gameRooms.get(roomId);
    }


    @Override
    public GameRoom playerQuitGame(String roomId, String playerName) throws GameException {
        GameRoom room = gameRooms.get(roomId);
        if (room == null) {
            throw new GameException("Room not found");
        }

        // Find and remove player from room
        Player playerToRemove = room.getPlayers().stream()
            .filter(p -> p.getName().equals(playerName))
            .findFirst()
            .orElseThrow(() -> new GameException("Player not found in room"));

        room.getPlayers().remove(playerToRemove);

        // Remove player from mapping
        playerRoomMap.remove(playerToRemove.getId());

        // If this was the host and there are other players, assign new host
        if (playerToRemove.isHost() && !room.getPlayers().isEmpty()) {
            Player newHost = room.getPlayers().get(0);
            newHost.setHost(true);
            room.setHostId(newHost.getId());
        }

        // If no players left, remove the room
        if (room.getPlayers().isEmpty()) {
            gameRooms.remove(roomId);
        }

        return room;
    }

    @Override
    public GameRoom restartGame(String roomId, String hostId) throws GameException {
        GameRoom room = gameRooms.get(roomId);
        if (room == null || !room.getHostId().equals(hostId)) {
            throw new GameException("Unauthorized or room not found");
        }

        if (room.getState() != GameState.FINISHED) {
            throw new GameException("Game is not finished");
        }

        // Reset game state to allow new players to join
        room.setState(GameState.WAITING_FOR_PLAYERS);
        room.setSecretNumber(0);
        room.setMinRange(1);
        room.setMaxRange(99);
        room.setCurrentPlayerIndex(0);
        if (room.getGameHistory() != null) {
            room.getGameHistory().clear();
        }

        return room;
    }

    @Override
    public GameRoom removePlayer(String roomId, String hostId, String playerName) throws GameException {
        GameRoom room = gameRooms.get(roomId);
        if (room == null || !room.getHostId().equals(hostId)) {
            throw new GameException("Unauthorized or room not found");
        }

        // Find and remove the specified player
        Player playerToRemove = room.getPlayers().stream()
            .filter(p -> p.getName().equals(playerName))
            .findFirst()
            .orElseThrow(() -> new GameException("Player not found in room"));

        // Prevent host from removing themselves
        if (playerToRemove.getId().equals(hostId)) {
            throw new GameException("Host cannot remove themselves");
        }

        room.getPlayers().remove(playerToRemove);
        playerRoomMap.remove(playerToRemove.getId());

        // If game is in progress and removed player was current, move to next
        if (room.getState() == GameState.IN_PROGRESS) {
            // Find the index of the removed player
            int removedPlayerIndex = -1;
            for (int i = 0; i < room.getPlayers().size() + 1; i++) {
                if (i == room.getCurrentPlayerIndex()) {
                    removedPlayerIndex = i;
                    break;
                }
            }

            // Adjust current player index if necessary
            if (removedPlayerIndex != -1 && removedPlayerIndex <= room.getCurrentPlayerIndex()) {
                if (room.getCurrentPlayerIndex() > 0) {
                    room.setCurrentPlayerIndex(room.getCurrentPlayerIndex() - 1);
                }
            }

            // Ensure current player index is valid
            if (room.getCurrentPlayerIndex() >= room.getPlayers().size()) {
                room.setCurrentPlayerIndex(0);
            }
        }

        return room;
    }


}
