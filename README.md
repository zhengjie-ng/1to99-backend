# 1to99 Backend

A real-time multiplayer number guessing game backend built with Spring Boot and WebSockets.

## 🎮 Game Description

1to99 is a competitive multiplayer guessing game where players take turns trying to avoid guessing a secret number between 1 and 99. The game provides feedback to narrow down the range with each guess. The objective is to force other players to guess the secret number - whoever guesses the exact secret number loses the game!

## 🔧 Technologies Used

- **Java 17** - Programming language
- **Spring Boot 3.5.6** - Web framework and application foundation
- **Spring WebSocket** - Real-time bidirectional communication
- **STOMP Protocol** - Simple Text Oriented Messaging Protocol over WebSockets
- **Spring Messaging** - Message handling and routing
- **Maven** - Build tool and dependency management
- **Lombok** - Reduces boilerplate code with annotations

## 📡 WebSocket Implementation

### What is WebSocket?

WebSocket is a communication protocol that provides full-duplex communication channels over a single TCP connection. Unlike traditional HTTP request-response patterns, WebSockets allow both the client and server to send data at any time, making them perfect for real-time applications like games, chat systems, and live updates.

### How This Project Uses WebSockets

This backend implements WebSockets using the **STOMP (Simple Text Oriented Messaging Protocol)** over WebSockets approach:

#### 1. WebSocket Configuration (`WebSocketConfig.java:11-31`)
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for topics and queues
        config.enableSimpleBroker("/topic", "/queue");
        // Set application destination prefix
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

#### 2. Message Endpoints (`GameController.java`)
The backend exposes several WebSocket endpoints:

- **`/app/createRoom`** - Creates a new game room
- **`/app/joinRoom`** - Joins an existing game room
- **`/app/startGameCountdown`** - Starts the game with countdown
- **`/app/makeGuess`** - Submits a number guess
- **`/app/quitGame`** - Leaves the current game
- **`/app/restartGame`** - Restarts the game
- **`/app/removePlayer`** - Removes a player (host only)

#### 3. Message Broadcasting
The server broadcasts updates to clients through:

- **`/topic/room.{roomId}`** - Room-specific updates (all players in room)
- **`/topic/user.{playerId}`** - Player-specific messages
- **`/queue/gameUpdate`** - Personal message queue

#### 4. Frontend-Backend Communication

The React Native frontend establishes a WebSocket connection to the backend:

1. **Connection**: Frontend connects to `/ws` endpoint using SockJS and STOMP
2. **Subscription**: Subscribes to relevant topics based on game state
3. **Messaging**: Sends actions and receives real-time game updates
4. **State Sync**: Game state stays synchronized across all connected clients

### Message Flow Example

1. **Player creates room**:
   - Frontend → `/app/createRoom` → Backend processes → Broadcast to `/topic/user.{tempPlayerId}`

2. **Player joins room**:
   - Frontend → `/app/joinRoom` → Backend updates room → Broadcast to `/topic/room.{roomId}`

3. **Player makes guess**:
   - Frontend → `/app/makeGuess` → Backend validates → Updates game state → Broadcast to all players

This real-time communication ensures all players see updates instantly without polling or page refreshes.

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Installation & Running

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd 1to99-backend
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application**
   - WebSocket endpoint: `ws://localhost:8080/ws`
   - Application runs on port `8080` by default

## 🌐 Live Demo Backend

**Don't want to set up your own backend?** You can use our hosted backend for testing and development:

- **Live Backend URL**: `https://1to99-backend.zhengjie.app/`
- **WebSocket Endpoint**: `wss://1to99-backend.zhengjie.app/ws`
- **Hosted on**: Synology NAS Container with Cloudflare Tunneling
- **Status**: Always available for development and testing

### Using the Live Backend

Simply configure your frontend to connect to:
```
wss://1to99-backend.zhengjie.app/ws
```

This allows you to:
- Test the frontend without running your own backend
- Focus on learning React Native/frontend development
- Experience the full multiplayer functionality immediately
- See how real-time WebSocket communication works

**Note**: This is a development/demo server - please don't use it for production applications.

### Configuration

The application can be configured through `application.properties`:

```properties
spring.application.name=1to99
logging.level.org.springframework.web.socket=DEBUG
logging.level.org.springframework.messaging=DEBUG
logging.level.org.springframework.web.socket.messaging=TRACE
logging.level.org.springframework.messaging.simp.stomp=TRACE
```

## 🎯 Game Features

- **Real-time multiplayer gameplay** - Multiple players can join and play simultaneously
- **Room-based system** - Players create or join specific game rooms
- **Turn-based guessing** - Players take turns guessing the secret number
- **Range narrowing** - Game provides "higher" or "lower" feedback to guide players
- **Host controls** - Room creators can start games, restart, and manage players
- **Auto-game ending** - Game automatically ends when the range narrows to a single number
- **Player management** - Hosts can remove disruptive players
- **Graceful disconnection** - Players can quit games cleanly

## 📁 Project Structure

```
src/main/java/com/_1to99/
├── config/
│   └── WebSocketConfig.java          # WebSocket configuration
├── controller/
│   ├── GameController.java           # WebSocket message handlers
│   └── SampleController.java         # HTTP REST endpoints
├── dto/                              # Data Transfer Objects
│   ├── CreateRoomMessage.java
│   ├── JoinRoomMessage.java
│   ├── GuessMessage.java
│   ├── QuitGameMessage.java
│   └── GameUpdateMessage.java
├── service/
│   ├── GameService.java              # Game business logic interface
│   └── impl/
│       └── GameServiceImpl.java      # Game service implementation
├── model/                            # Game entities
└── Application.java                  # Spring Boot main class
```

## 🔗 Related Projects

This backend works in conjunction with the **1to99 Mobile Frontend** - a React Native application that provides the user interface for the game.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.