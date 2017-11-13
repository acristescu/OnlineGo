package io.zenandroid.onlinego.model;

import android.graphics.Point;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import io.zenandroid.onlinego.Util;

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

    public Set<Point> getAllStonesCoordinates() {
        return stones.keySet();
    }

    /**
     * Morph this postion to a new one by performing the move specified.
     * If the move is invalid, no action is taken and the method returns false.
     * If the move results in a capture, the captured group is removed from the
     * position.
     * @param stone
     * @param where
     * @return
     */
    public boolean makeMove(StoneType stone, Point where) {
        if(getStoneAt(where.x, where.y) != null) {
            //
            // Can't place a stone on top of another
            //
            return false;
        }

        putStone(where.x, where.y, stone);

        //
        // Determine if the newly placed stone results in a capture.
        // For this, we're calling doCapture() on all the neighbours
        // of the new stones that are of opposite color
        //
        List<Point> removedStones = new LinkedList<>();
        List<Point> neighbours = Util.getNeighbouringSpace(where, boardSize);

        for(Point neighbour : neighbours) {
            StoneType neighbourType = stones.get(neighbour);
            if(neighbourType != null && neighbourType != stone) {
                List<Point> removedGroup = doCapture(neighbour, neighbourType);
                if(removedGroup != null) {
                    removedStones.addAll(removedGroup);
                }
            }
        }

        if(!removedStones.isEmpty()) {
            for(Point p : removedStones) {
                stones.remove(p);
            }
        } else {
            //
            // We need to check for suicide
            //
            List<Point> suicideGroup = doCapture(where, stone);
            if(suicideGroup != null && !suicideGroup.isEmpty()) {
                stones.remove(where);
                return false;
            }
        }

        lastMove = where;
        return true;
    }

    /**
     * Check if the stone group that contains the stone passed as a
     * parameter is completely surrounded by opposing pieces and the edges
     * of the board. If so, the entire group is returned
     *
     * @param origin
     * @param type
     * @return
     */
    private List<Point> doCapture(Point origin, StoneType type) {
        //
        // For this, we're using a simplified shape recognition mechanism
        // For each visited node, we're getting all the neighbours, checking
        // to see if any of them is empty (in which case we immediately exit,
        // because it means we have no capture) and if not, we add the
        // neighbours of the same color with the original to the toVisit list
        // Lastly, we move the visited node from the toVisit to the visited list
        // At the end, if we did not return before the toVisit list is empty
        // it means the group is surrounded and the contents of the visited
        // list is returned.
        //
        LinkedList<Point> toVisit = new LinkedList<>();
        LinkedList<Point> visited = new LinkedList<>();

        toVisit.add(origin);

        while(!toVisit.isEmpty()) {
            Point current = toVisit.pop();
            visited.add(current);
            List<Point> neighbours = Util.getNeighbouringSpace(current, boardSize);

            for(Point toCheck : neighbours) {
                StoneType checkedStoneType = stones.get(toCheck);
                if(checkedStoneType == null) {
                    //
                    // A liberty, hence no capture
                    //
                    return null;
                }
                if(checkedStoneType == type && !visited.contains(toCheck)) {
                    toVisit.add(toCheck);
                }
            }
        }

        return visited;
    }

    public Point getLastMove() {
        return lastMove;
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
        return newPos;
    }
}
