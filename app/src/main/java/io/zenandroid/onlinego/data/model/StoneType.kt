package io.zenandroid.onlinego.data.model;

import androidx.compose.runtime.Immutable;

/**
 * Created by alex on 1/9/2015.
 */
@Immutable
public enum StoneType {
    BLACK, WHITE;

    public StoneType getOpponent() {
        return this == BLACK ? WHITE : BLACK;
    }
}
