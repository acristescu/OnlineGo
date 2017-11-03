package io.zenandroid.onlinego.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import io.zenandroid.onlinego.model.Position;
import io.zenandroid.onlinego.model.StoneType;

/**
 * Created by alex on 1/8/2015.
 * This is a view that is capable of displaying a GO board and a GO game position
 * that is passed to it via setPosition()
 */
public class Board extends View {
    //
    // The logical size (in GO nodes) of the board.
    // Supported sizes: 9, 13 and 19
    //
    private int boardSize = 19;

    //
    // Size of border between edge and first line
    //
    private float border = 0;

    private Paint linesPaint = new Paint();

    //
    // Size (in px) of the cell
    //
    private int cellSize;
    private Position position;

    public Board(Context context) {
        super(context);
        init(context);
    }

    public Board(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Board(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        linesPaint = new Paint();
        linesPaint.setColor(0xFF000000);
        linesPaint.setAntiAlias(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //
        // We're enforcing the board to be always square
        //
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int size;
        if(widthMode == MeasureSpec.EXACTLY && widthSize > 0){
            size = widthSize;
        } else if(heightMode == MeasureSpec.EXACTLY && heightSize > 0){
            size = heightSize;
        } else {
            size = widthSize < heightSize ? widthSize : heightSize;
        }

        int finalMeasureSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
        super.onMeasure(finalMeasureSpec, finalMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //
        // We need to adapt to resize events by recomputing the border and cell sizes
        //
        cellSize = w / boardSize;
        border = (w - boardSize * cellSize) / 2;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.translate(border, border);

        drawBackground(canvas);
        drawGrid(canvas);
        drawStarPoints(canvas);
        drawCoordinates(canvas);
        if(position != null) {
            drawStones(canvas);
            drawDecorations(canvas);
        }
    }

    /**
     * Draws the "star points" (e.g. place where handicap stones can be placed).
     * These are fixed for each board size.
     * @param canvas
     */
    private void drawStarPoints(Canvas canvas) {
        switch(boardSize) {
            case 19:
                drawSingleStarPoint(canvas, 3, 3);
                drawSingleStarPoint(canvas, 15, 15);
                drawSingleStarPoint(canvas, 3, 15);
                drawSingleStarPoint(canvas, 15, 3);

                drawSingleStarPoint(canvas, 3, 9);
                drawSingleStarPoint(canvas, 9, 3);
                drawSingleStarPoint(canvas, 15, 9);
                drawSingleStarPoint(canvas, 9, 15);

                drawSingleStarPoint(canvas, 9, 9);
                break;
            case 13:
                drawSingleStarPoint(canvas, 3, 3);
                drawSingleStarPoint(canvas, 9, 9);
                drawSingleStarPoint(canvas, 3, 9);
                drawSingleStarPoint(canvas, 9, 3);

                drawSingleStarPoint(canvas, 6, 6);
                break;
            case 9:
                drawSingleStarPoint(canvas, 2, 2);
                drawSingleStarPoint(canvas, 6, 6);
                drawSingleStarPoint(canvas, 2, 6);
                drawSingleStarPoint(canvas, 6, 2);

                drawSingleStarPoint(canvas, 4, 4);
                break;
        }
    }

    private void drawSingleStarPoint(Canvas canvas, int i, int j) {
        PointF center = getCellCenter(i, j);
        canvas.drawRect(center.x-4, center.y-4, center.x + 5, center.y + 5, linesPaint);
    }

    private void drawGrid(Canvas canvas) {
        for(int i = 0; i< boardSize; i++) {
            //
            // As an optimisation, we're taking advantage of the fact that
            // the board is square and draw a horizontal and a vertical line
            // with each iteration
            //
            float halfCell = cellSize / 2f;
            float start = i * cellSize + halfCell;
            float fullLength = cellSize * boardSize;
            canvas.drawLine(start, halfCell, start, fullLength - halfCell, linesPaint);
            canvas.drawLine(halfCell, start, fullLength - halfCell, start, linesPaint);
        }
    }

    private void drawCoordinates(Canvas canvas) {
        // TODO
    }

    private void drawStones(Canvas canvas) {
        if(position == null) {
            return;
        }
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        for(Point p : position.getAllStonesCoordinates()) {
            StoneType type = position.getStoneAt(p.x, p.y);

            paint.setColor(type == StoneType.WHITE? Color.WHITE: Color.BLACK);
            paint.setStyle(Paint.Style.FILL);

            PointF center = getCellCenter(p.x, p.y);
            canvas.drawCircle(center.x, center.y, cellSize / 2f - 1, paint);

            //
            // For white stones, we also draw a black outline
            //
            if(type == StoneType.WHITE) {
                paint.setColor(Color.BLACK);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(center.x, center.y, cellSize / 2f - 1, paint);
            }
        }
    }

    /**
     * Draw decorations, such as the last move indicator. In the future,
     * the position object might contain other decorations, such as dead
     * stone or territory indicators.
     * @param canvas
     */
    private void drawDecorations(Canvas canvas) {
        Point lastMove = position.getLastMove();
        if(lastMove != null) {
            StoneType type = position.getStoneAt(lastMove.x, lastMove.y);
            PointF center = getCellCenter(lastMove.x, lastMove.y);

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(type == StoneType.WHITE? Color.BLACK: Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            canvas.drawCircle(center.x, center.y, cellSize / 4, paint);
        }

        //TODO: other decorations
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawARGB(255, 0xDE, 0xB0, 0x66);
    }

    private PointF getCellCenter(int i, int j) {
        PointF center = new PointF();
        float halfCell = cellSize / 2f;
        center.set(i * cellSize + halfCell, j * cellSize + halfCell);

        return center;
    }

    public void setPosition(Position position) {
        this.position = position;
        invalidate();
    }

    public Position getPosition() {
        return position;
    }
}
