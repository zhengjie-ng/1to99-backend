package com._1to99.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GameRoom {
    private String roomId;
    private String hostId;
    private List<Player> players;
    private GameState state;
    private int secretNumber;
    private int currentPlayerIndex;
    private int minRange;
    private int maxRange;
    private List<GameTurn> gameHistory;
    
}
