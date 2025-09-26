package com._1to99.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinRoomMessage {
    private String roomId;
    private String playerName;
}
