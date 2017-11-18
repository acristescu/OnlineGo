package io.zenandroid.onlinego.model;

import android.graphics.Point;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Set;

/**
 * A class that models a GO position. It is basically a collection of stones
 * that are placed on the intersections of the lines of a GO board.
 * For quick retrieval, the stones are kept as a HashMap (a hashtable).
 *
 * To determine if a stone is present at a position (and which kind) a user
 * can use the getStoneAt() method.
 *
 * To morph the position one can use the makeMove() method. This method is responsible
 * for validating the move and changing the Position to the new one by enforcing
 * captures for example.
 *
 * Lastly, the user can use getAllStonesCoordinates() to loop throught all the stones.
 *
 * Created by alex on 1/8/2015.
 */
public class Position {

    private HashMap<Point, StoneType> stones = new HashMap<>();
    private int boardSize = 19;
    private Point lastMove = null;
    private int whiteCapturedCount = 0;
    private int blackCapturedCount = 0;

    public Position(int boardSize) {
        this.boardSize = boardSize;
    }

    /**
     * Adds a stone without checking the game logic. See makeMove() for alternative
     * @param i
     * @param j
     * @param type
     */
    public void putStone(int i, int j, StoneType type) {
       stones.put(new Point(i, j), type);
   }

    /**
     * Returns the type of stone at the specified intersection or null if there is
     * no stone there.
     * @param i
     * @param j
     * @return
     */
    public StoneType getStoneAt(int i, int j) {
        return stones.get(new Point(i,j));
    }

    public StoneType getStoneAt(Point p) {
        return stones.get(p);
    }

    public Set<Point> getAllStonesCoordinates() {
        return stones.keySet();
    }

    public Point getLastMove() {
        return lastMove;
    }

    public void setLastMove(Point lastMove) {
        this.lastMove = lastMove;
    }

    public StoneType getLastPlayerToMove() {
        if(lastMove == null) {
            return null;
        }

        return getStoneAt(lastMove.x, lastMove.y);
    }

    public Position clone() {
        final Position newPos = new Position(boardSize);
        newPos.stones.putAll(stones);
        newPos.lastMove = lastMove;
        newPos.blackCapturedCount = blackCapturedCount;
        newPos.whiteCapturedCount = whiteCapturedCount;
        return newPos;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public void removeStone(@NotNull Point p) {
        stones.remove(p);
    }

    public int getWhiteCapturedCount() {
        return whiteCapturedCount;
    }

    public void setWhiteCapturedCount(int whiteCapturedCount) {
        this.whiteCapturedCount = whiteCapturedCount;
    }

    public int getBlackCapturedCount() {
        return blackCapturedCount;
    }

    public void setBlackCapturedCount(int blackCapturedCount) {
        this.blackCapturedCount = blackCapturedCount;
    }
}
