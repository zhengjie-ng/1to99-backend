package com._1to99.controller;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com._1to99.dto.CreateRoomMessage;
import com._1to99.dto.GameUpdateMessage;
import com._1to99.dto.GuessMessage;
import com._1to99.dto.JoinRoomMessage;
import com._1to99.dto.QuitGameMessage;
import com._1to99.exception.GameException;
import com._1to99.model.GameRoom;
import com._1to99.model.GameTurn;
import com._1to99.model.Player;
import com._1to99.service.GameService;

@Controller
public class GameController {
    
    @Autowired
    private GameService gameService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/createRoom")
    public void createRoom(@Payload CreateRoomMessage message, SimpMessageHeaderAccessor headerAccessor) {
        System.out.println("DEBUG: ===== CREATE ROOM METHOD CALLED =====");
        System.out.println("DEBUG: Received message: " + message);
        System.out.println("DEBUG: Player name: " + (message != null ? message.getPlayerName() : "NULL MESSAGE"));
        try {
            GameRoom room = gameService.createRoom(message.getPlayerName());
            System.out.println("DEBUG: Room created successfully: " + room.getRoomId());
            
            // Store player ID in session
            headerAccessor.getSessionAttributes().put("playerId", room.getHostId());
            
            // Send room created confirmation to the host
            GameUpdateMessage update = new GameUpdateMessage();
            update.setType("ROOM_CREATED");
            update.setGameRoom(room);
            update.setMessage("Room created successfully");
            
            String sessionId = headerAccessor.getSessionId();
            System.out.println("DEBUG: SessionId: " + sessionId);

            // Send to the specific user's personal topic using tempPlayerId if available
            String userTopicId = message.getTempPlayerId() != null ? message.getTempPlayerId() : room.getHostId();
            System.out.println("DEBUG: Sending to user-specific topic: /topic/user." + userTopicId);
            messagingTemplate.convertAndSend("/topic/user." + userTopicId, update);
            System.out.println("DEBUG: Response sent to user-specific topic successfully");

            // Also send to room topic for future messages
            System.out.println("DEBUG: Also sending to room topic: /topic/room." + room.getRoomId());
            messagingTemplate.convertAndSend("/topic/room." + room.getRoomId(), update);
            System.out.println("DEBUG: Response sent to room topic successfully");

        } catch (Exception e) {
            System.out.println("DEBUG: Exception occurred in createRoom: " + e.getMessage());
            e.printStackTrace();
            String sessionId = headerAccessor.getSessionId();
            if (sessionId != null) {
                sendError(sessionId, e.getMessage());
            }
        }
    }

    @MessageMapping("/joinRoom")
    public void joinRoom(@Payload JoinRoomMessage message, SimpMessageHeaderAccessor headerAccessor) {
        System.out.println("DEBUG: ===== JOIN ROOM METHOD CALLED =====");
        System.out.println("DEBUG: Room ID: " + message.getRoomId());
        System.out.println("DEBUG: Player name: " + message.getPlayerName());
        try {
            GameRoom room = gameService.joinRoom(message.getRoomId(), message.getPlayerName());
            System.out.println("DEBUG: Player joined successfully, room now has " + room.getPlayers().size() + " players");
            
            // Find the new player and store ID in session
            String playerId = room.getPlayers().stream()
                .filter(p -> p.getName().equals(message.getPlayerName()))
                .findFirst()
                .map(Player::getId)
                .orElse(null);
            
            headerAccessor.getSessionAttributes().put("playerId", playerId);
            
            // Broadcast player joined to all players in the room
            GameUpdateMessage update = new GameUpdateMessage();
            update.setType("PLAYER_JOINED");
            update.setGameRoom(room);
            update.setMessage(message.getPlayerName() + " joined the game");
            
            System.out.println("DEBUG: Broadcasting PLAYER_JOINED to room topic: /topic/room." + room.getRoomId());
            messagingTemplate.convertAndSend("/topic/room." + room.getRoomId(), update);
            System.out.println("DEBUG: PLAYER_JOINED broadcast sent successfully");

        } catch (Exception e) {
            System.out.println("DEBUG: Exception in joinRoom: " + e.getMessage());
            e.printStackTrace();
            String sessionId = headerAccessor.getSessionId();
            if (sessionId != null) {
                sendError(sessionId, e.getMessage());
                // Also send to general topic to ensure frontend receives it
                GameUpdateMessage errorUpdate = new GameUpdateMessage();
                errorUpdate.setType("ERROR");
                errorUpdate.setMessage(e.getMessage());
                messagingTemplate.convertAndSend("/topic/gameResponse", errorUpdate);
                System.out.println("DEBUG: Error also sent to /topic/gameResponse");
            }
        }
    }

    @MessageMapping("/startGameCountdown")
    public void startGameCountdown(@Payload Map<String, String> message, SimpMessageHeaderAccessor headerAccessor) {
        System.out.println("DEBUG: ===== START GAME COUNTDOWN CALLED =====");
        try {
            String roomId = message.get("roomId");
            String playerId = (String) headerAccessor.getSessionAttributes().get("playerId");

            GameRoom room = gameService.getRoom(roomId);
            if (room == null || !room.getHostId().equals(playerId)) {
                throw new GameException("Unauthorized or room not found");
            }

            // Broadcast countdown start to all players
            GameUpdateMessage update = new GameUpdateMessage();
            update.setType("GAME_STARTING_COUNTDOWN");
            update.setGameRoom(room);
            update.setMessage("Game starting in 5 seconds...");

            System.out.println("DEBUG: Broadcasting countdown to room: " + roomId);
            messagingTemplate.convertAndSend("/topic/room." + roomId, update);

            // Schedule the actual game start after 5 seconds
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        GameRoom startedRoom = gameService.startGame(roomId, playerId);

                        GameUpdateMessage startUpdate = new GameUpdateMessage();
                        startUpdate.setType("GAME_STARTED");
                        startUpdate.setGameRoom(startedRoom);
                        startUpdate.setMessage("Game started! Current range: 1-99");

                        messagingTemplate.convertAndSend("/topic/room." + roomId, startUpdate);
                        System.out.println("DEBUG: Game started successfully after countdown");
                    } catch (Exception e) {
                        System.err.println("Error starting game after countdown: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }, 5000);

        } catch (Exception e) {
            System.out.println("DEBUG: Exception in startGameCountdown: " + e.getMessage());
            e.printStackTrace();
            String sessionId = headerAccessor.getSessionId();
            if (sessionId != null) {
                sendError(sessionId, e.getMessage());
            }
        }
    }

    @MessageMapping("/startGame")
    public void startGame(@Payload Map<String, String> message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String roomId = message.get("roomId");
            String playerId = (String) headerAccessor.getSessionAttributes().get("playerId");
            
            GameRoom room = gameService.startGame(roomId, playerId);
            
            GameUpdateMessage update = new GameUpdateMessage();
            update.setType("GAME_STARTED");
            update.setGameRoom(room);
            update.setMessage("Game started! Current range: 1-99");
            
            messagingTemplate.convertAndSend("/topic/room." + roomId, update);

        } catch (Exception e) {
            String sessionId = headerAccessor.getSessionId();
            if (sessionId != null) {
                sendError(sessionId, e.getMessage());
            }
        }
    }

    @MessageMapping("/makeGuess")
    public void makeGuess(@Payload GuessMessage message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String playerId = (String) headerAccessor.getSessionAttributes().get("playerId");
            GameTurn turn = gameService.makeGuess(message.getRoomId(), playerId, message.getGuess());
            
            GameRoom room = gameService.getRoom(message.getRoomId());
            
            GameUpdateMessage update = new GameUpdateMessage();
            update.setType("GUESS_MADE");
            update.setGameRoom(room);
            update.setLastTurn(turn);
            update.setMessage(turn.getPlayerName() + " guessed " + turn.getGuess());
            
            messagingTemplate.convertAndSend("/topic/room." + message.getRoomId(), update);

        } catch (Exception e) {
            String sessionId = headerAccessor.getSessionId();
            if (sessionId != null) {
                sendError(sessionId, e.getMessage());
            }
        }
    }


    @MessageMapping("/quitGame")
    public void quitGame(@Payload QuitGameMessage message, SimpMessageHeaderAccessor headerAccessor) {
        System.out.println("DEBUG: ===== QUIT GAME METHOD CALLED =====");
        System.out.println("DEBUG: Room ID: " + message.getRoomId());
        System.out.println("DEBUG: Player name: " + message.getPlayerName());
        try {
            GameRoom room = gameService.playerQuitGame(message.getRoomId(), message.getPlayerName());
            System.out.println("DEBUG: Player removed successfully from room");

            if (room.getPlayers().isEmpty()) {
                System.out.println("DEBUG: Room is now empty, it has been removed");
                // Room was removed, no need to broadcast
                return;
            }

            // Send player quit update
            GameUpdateMessage update = new GameUpdateMessage();
            update.setType("PLAYER_QUIT");
            update.setGameRoom(room);
            update.setMessage(message.getPlayerName() + " left the game");

            System.out.println("DEBUG: Broadcasting PLAYER_QUIT to room topic: /topic/room." + room.getRoomId());
            messagingTemplate.convertAndSend("/topic/room." + room.getRoomId(), update);

        } catch (Exception e) {
            System.out.println("DEBUG: Exception in quitGame: " + e.getMessage());
            e.printStackTrace();
            String sessionId = headerAccessor.getSessionId();
            if (sessionId != null) {
                sendError(sessionId, e.getMessage());
            }
        }
    }

    @MessageMapping("/restartGame")
    public void restartGame(@Payload Map<String, String> message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String roomId = message.get("roomId");
            String playerId = (String) headerAccessor.getSessionAttributes().get("playerId");

            GameRoom room = gameService.restartGame(roomId, playerId);

            GameUpdateMessage update = new GameUpdateMessage();
            update.setType("GAME_RESTARTED");
            update.setGameRoom(room);
            update.setMessage("Game has been restarted. Waiting for players to join!");

            messagingTemplate.convertAndSend("/topic/room." + roomId, update);

        } catch (Exception e) {
            String sessionId = headerAccessor.getSessionId();
            if (sessionId != null) {
                sendError(sessionId, e.getMessage());
            }
        }
    }

    @MessageMapping("/removePlayer")
    public void removePlayer(@Payload Map<String, String> message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String roomId = message.get("roomId");
            String playerName = message.get("playerName");
            String hostId = (String) headerAccessor.getSessionAttributes().get("playerId");

            // Get the room before removal to find the player's ID
            GameRoom roomBefore = gameService.getRoom(roomId);
            String removedPlayerId = null;
            if (roomBefore != null) {
                removedPlayerId = roomBefore.getPlayers().stream()
                    .filter(p -> p.getName().equals(playerName))
                    .findFirst()
                    .map(p -> p.getId())
                    .orElse(null);
            }

            GameRoom room = gameService.removePlayer(roomId, hostId, playerName);

            // Send notification to removed player first
            if (removedPlayerId != null) {
                GameUpdateMessage kickedMessage = new GameUpdateMessage();
                kickedMessage.setType("PLAYER_KICKED");
                kickedMessage.setMessage("You have been removed from the game by the host");

                // Try to send to user-specific topic
                messagingTemplate.convertAndSend("/topic/user." + removedPlayerId, kickedMessage);
            }

            // Send update to remaining players in the room
            GameUpdateMessage update = new GameUpdateMessage();
            update.setType("PLAYER_REMOVED");
            update.setGameRoom(room);
            update.setMessage(playerName + " was removed from the game by the host");

            messagingTemplate.convertAndSend("/topic/room." + roomId, update);

        } catch (Exception e) {
            String sessionId = headerAccessor.getSessionId();
            if (sessionId != null) {
                sendError(sessionId, e.getMessage());
            }
        }
    }


    private void sendError(String sessionId, String errorMessage) {
        GameUpdateMessage error = new GameUpdateMessage();
        error.setType("ERROR");
        error.setMessage(errorMessage);
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/gameUpdate", error);
    }
}
