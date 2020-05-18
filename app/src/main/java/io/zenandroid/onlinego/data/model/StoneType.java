package io.zenandroid.onlinego.data.model;

/**
 * Created by alex on 1/9/2015.
 */
public enum StoneType {
    BLACK, WHITE;

    public StoneType getOpponent() {
        return this == BLACK ? WHITE : BLACK;
    }
}
