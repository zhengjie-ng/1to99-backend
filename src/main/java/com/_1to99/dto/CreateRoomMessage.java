package com._1to99.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoomMessage {
    private String playerName;
    private String tempPlayerId;
}