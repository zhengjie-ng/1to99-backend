package com._1to99.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GuessMessage {
    private String roomId;
    private int guess;
}
