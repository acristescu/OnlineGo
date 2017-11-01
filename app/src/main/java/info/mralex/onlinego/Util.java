package info.mralex.onlinego;

import android.graphics.Point;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by alex on 1/9/2015.
 */
public class Util {

    public static String getSGFCoordinates(int i, int j) {
        char column = (char)('a' + i);
        char row = (char)('a' + i);
        return new String(new char[]{column, row});
    }

    public static Point getCoordinatesFromSGF(String sgf, int offset) {
        char column = sgf.charAt(offset);
        char row = sgf.charAt(offset+1);

        Point point = new Point();
        point.x = column - 'a';
        point.y = row - 'a';

        return point;
    }

    public static List<Point> getNeighbouringSpace(Point current, int boardSize) {
        Point left = new Point(current);
        left.offset(-1, 0);
        Point right = new Point(current);
        right.offset(1, 0);
        Point up = new Point(current);
        up.offset(0, -1);
        Point down = new Point(current);
        down.offset(0, 1);

        LinkedList<Point> list = new LinkedList<>();
        if(up.x >= 0 && up.x < boardSize && up.y >= 0 && up.y < boardSize) {
            list.add(up);
        }
        if(down.x >= 0 && down.x < boardSize && down.y >= 0 && down.y < boardSize) {
            list.add(down);
        }
        if(left.x >= 0 && left.x < boardSize && left.y >= 0 && left.y < boardSize) {
            list.add(left);
        }
        if(right.x >= 0 && right.x < boardSize && right.y >= 0 && right.y < boardSize) {
            list.add(right);
        }

        return list;
    }
}
