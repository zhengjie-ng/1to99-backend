package com._1to99.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Player {
    private String id;
    private String name;
    private boolean isHost;
    private String postGameDecision; // "PLAY_AGAIN", "QUIT", or null for no decision yet

    public Player(String id, String name, boolean isHost) {
        this.id = id;
        this.name = name;
        this.isHost = isHost;
        this.postGameDecision = null;
    }
}
