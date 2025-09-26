package com._1to99.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GameTurn {
    private String playerId;
    private String playerName;
    private int guess;
    private String result;
    private long timestamp;
    

}
