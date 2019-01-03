package io.zenandroid.onlinego.views

import android.content.Context
import android.graphics.*
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.model.StoneType
import java.util.*
import kotlin.math.roundToInt


/**
 * Created by alex on 1/8/2015.
 * This is a view that is capable of displaying a GO board and a GO game position
 * that is passed to it via setPosition()
 */
class BoardView : View {
    //
    // The logical size (in GO nodes) of the board.
    // Supported sizes: 9, 13 and 19
    //
    var prefs = PreferenceManager.getDefaultSharedPreferences(OnlineGoApplication.instance.getBaseContext())
    var isGameCard = false

    var boardSize = 19
        set(boardSize) {
            field = boardSize
            var usableWidth = width // available width will be adjusted when coordinates are active
            if (prefs.getBoolean("show_coordinates", false) && !isGameCard) {
                usableWidth -= (width / this.boardSize.toFloat() * 0.65).roundToInt()

            }
            cellSize = usableWidth / boardSize
            border = ((width - boardSize * cellSize) / 2).toFloat()
        }

    var position: Position? = null
        set(position) {
            field = position
            invalidate()
        }
    var isInteractive = false
    var drawLastMove = true
        set(value) {
            field = value
            invalidate()
        }
    var drawTerritory = false
        set(value) {
            field = value
            invalidate()
        }
    var fadeOutRemovedStones = false
        set(value) {
            field = value
            invalidate()
        }

    //
    // Size of border between edge and first line
    //
    private var border = 0f

    private var linesPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var linesHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val decorationsPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val coordinatesPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val territoryPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val whiteStoneDrawable = resources.getDrawable(R.mipmap.stone_white)
    private val blackStoneDrawable = resources.getDrawable(R.mipmap.stone_black)
    private val shadowDrawable = resources.getDrawable(R.drawable.gradient_shadow)

    private val textBounds = Rect()

    //
    // Size (in px) of the cell
    //
    private var cellSize: Int = 0
    private var stoneSpacing = 0f
    private var candidateMove: Point? = null
    private var candidateType: StoneType? = null

    private val tapUpSubject = PublishSubject.create<Point>()
    private val tapMoveSubject = PublishSubject.create<Point>()


    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        linesPaint.strokeWidth = 2f
        linesPaint.color = -0x33cce7f0
        linesPaint.strokeCap = Paint.Cap.ROUND

        linesHighlightPaint.strokeWidth = 4f
        linesHighlightPaint.color = resources.getColor(R.color.white)
        linesHighlightPaint.strokeCap = Paint.Cap.ROUND

        decorationsPaint.style = Paint.Style.STROKE
        decorationsPaint.strokeWidth = 3f

        territoryPaint.strokeWidth = 4f
        
        if(texture == null) {
            texture = BitmapFactory.decodeResource(resources, R.mipmap.texture)
        }
    }

    private fun drawTextCentred(canvas: Canvas, paint: Paint, text: String, cx: Float, cy: Float, ignoreAscentDescent: Boolean = false) {
        paint.textAlign = Paint.Align.LEFT
        paint.getTextBounds(text, 0, text.length, textBounds)
        canvas.drawText(text,
                cx - textBounds.exactCenterX() ,
                cy - textBounds.exactCenterY() + if (ignoreAscentDescent) (textBounds.bottom.toFloat() / 2) else 0f,
                paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isInteractive) {
            return super.onTouchEvent(event)
        }
        val eventCoords = screenToBoardCoordinates(event.x, event.y)

        tapMoveSubject.onNext(eventCoords)

        if (event.action == MotionEvent.ACTION_UP) {
            tapUpSubject.onNext(eventCoords)
            lastHotTrackedPoint = null
        }

        return true
    }

    private fun screenToBoardCoordinates(x: Float, y: Float): Point {
        return Point(
                ((x - border).toInt() / cellSize).coerceIn(0, boardSize - 1),
                ((y - border).toInt() / cellSize).coerceIn(0, boardSize - 1)
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //
        // We're enforcing the board to be always square
        //
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)

        val size = if (widthMode == View.MeasureSpec.EXACTLY && widthSize > 0) {
            widthSize
        } else if (heightMode == View.MeasureSpec.EXACTLY && heightSize > 0) {
            heightSize
        } else {
            if (widthSize < heightSize) widthSize else heightSize
        }

        val finalMeasureSpec = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
        super.onMeasure(finalMeasureSpec, finalMeasureSpec)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        //
        // We need to adapt to resize events by recomputing the border and cell sizes
        //
        var usableWidth = w // available width will be adjusted when coordinates are active
        if (prefs.getBoolean("show_coordinates", false) && !isGameCard) {
            usableWidth -= (w / this.boardSize.toFloat() * 0.65).roundToInt()
        }
        cellSize = usableWidth / this.boardSize
        linesPaint.strokeWidth = (cellSize / 35f).coerceAtMost(2f)
        linesHighlightPaint.strokeWidth = linesPaint.strokeWidth * 2
        decorationsPaint.strokeWidth = cellSize / 20f
        stoneSpacing = cellSize / 35f
        border = ((w - this.boardSize * cellSize) / 2).toFloat()
        textPaint.textSize = cellSize.toFloat() * .65f
        coordinatesPaint.textSize = cellSize.toFloat() * .35f
        coordinatesPaint.typeface = Typeface.DEFAULT_BOLD
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawBackground(canvas)
        canvas.translate(border, border)


        drawGrid(canvas)
        drawStarPoints(canvas)
        drawCoordinates(canvas)
        position?.let {
            drawStones(canvas, it)
            drawDecorations(canvas, it)
            drawTerritory(canvas, it)
        }
        candidateMove?.let {
            drawSelection(canvas, it)
        }
    }

    private fun drawTerritory(canvas: Canvas, position: Position) {
        if(!drawTerritory) {
            return
        }
        for(i in 0 until boardSize) {
            for(j in 0 until boardSize) {
                val p = Point(i, j)
                val center = getCellCenter(i, j)
                if(position.whiteTerritory.contains(p)) {
                    territoryPaint.color = Color.WHITE
                } else if(position.blackTerritory.contains(p)) {
                    territoryPaint.color = Color.BLACK
                } else if(position.getStoneAt(p) == null && position.removedSpots.contains(p)) {
                    territoryPaint.color = Color.TRANSPARENT
                } else {
                    continue
                }
                territoryPaint.style = Paint.Style.FILL
                canvas.drawRect(
                        center.x - (cellSize / 8).toFloat(),
                        center.y - (cellSize / 8).toFloat(),
                        center.x + (cellSize / 8).toFloat(),
                        center.y + (cellSize / 8).toFloat(),
                        territoryPaint)
                territoryPaint.style = Paint.Style.STROKE
                territoryPaint.color = Color.GRAY
                canvas.drawRect(
                        center.x - (cellSize / 8).toFloat(),
                        center.y - (cellSize / 8).toFloat(),
                        center.x + (cellSize / 8).toFloat(),
                        center.y + (cellSize / 8).toFloat(),
                        territoryPaint)

            }
        }
    }

    private fun drawSelection(canvas: Canvas, candidateMove: Point) {
        val center = getCellCenter(candidateMove.x, candidateMove.y)
        val drawable = if (candidateType == StoneType.BLACK) blackStoneDrawable else whiteStoneDrawable
        drawable.setBounds(
                (center.x - cellSize / 2f + stoneSpacing).toInt(),
                (center.y - cellSize / 2f + stoneSpacing).toInt(),
                (center.x + cellSize / 2f - stoneSpacing).toInt(),
                (center.y + cellSize / 2f - stoneSpacing).toInt()
        )
        drawable.alpha = 100
        drawable.draw(canvas)
    }

    /**
     * Draws the "star points" (e.g. place where handicap stones can be placed).
     * These are fixed for each board size.
     * @param canvas
     */
    private fun drawStarPoints(canvas: Canvas) {
        when (this.boardSize) {
            19 -> {
                drawSingleStarPoint(canvas, 3, 3)
                drawSingleStarPoint(canvas, 15, 15)
                drawSingleStarPoint(canvas, 3, 15)
                drawSingleStarPoint(canvas, 15, 3)

                drawSingleStarPoint(canvas, 3, 9)
                drawSingleStarPoint(canvas, 9, 3)
                drawSingleStarPoint(canvas, 15, 9)
                drawSingleStarPoint(canvas, 9, 15)

                drawSingleStarPoint(canvas, 9, 9)
            }
            13 -> {
                drawSingleStarPoint(canvas, 3, 3)
                drawSingleStarPoint(canvas, 9, 9)
                drawSingleStarPoint(canvas, 3, 9)
                drawSingleStarPoint(canvas, 9, 3)

                drawSingleStarPoint(canvas, 6, 6)
            }
            9 -> {
                drawSingleStarPoint(canvas, 2, 2)
                drawSingleStarPoint(canvas, 6, 6)
                drawSingleStarPoint(canvas, 2, 6)
                drawSingleStarPoint(canvas, 6, 2)

                drawSingleStarPoint(canvas, 4, 4)
            }
        }
    }

    private fun drawSingleStarPoint(canvas: Canvas, i: Int, j: Int) {
        val center = getCellCenter(i, j)
        canvas.drawCircle(center.x, center.y, cellSize / 15f, linesPaint)
    }

    private fun drawGrid(canvas: Canvas) {
        for (i in 0 until this.boardSize) {
            //
            // As an optimisation, we're taking advantage of the fact that
            // the board is square and draw a horizontal and a vertical line
            // with each iteration
            //
            val halfCell = cellSize / 2f
            val start = i * cellSize + halfCell
            val fullLength = (cellSize * this.boardSize).toFloat()

            canvas.drawLine(start, halfCell, start, fullLength - halfCell, linesPaint)
            canvas.drawLine(halfCell, start, fullLength - halfCell, start, linesPaint)
            if((i == candidateMove?.x)) {
                canvas.drawLine(start, halfCell, start, fullLength - halfCell, linesHighlightPaint)
            }
            if(i == candidateMove?.y) {
                canvas.drawLine(halfCell, start, fullLength - halfCell, start, linesHighlightPaint)
            }
        }
    }

    private fun drawCoordinates(canvas: Canvas) {
        if (prefs.getBoolean("show_coordinates", false) && !isGameCard) {
            val fullLength = (cellSize * this.boardSize).toFloat()
            val textX = arrayOf("A","B","C","D","E","F","G","H","J","K","L","M","N","O","P","Q","R","S","T")
            val textY = arrayOf("1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19")
            for (i in 0 until this.boardSize) {
                drawTextCentred(canvas, coordinatesPaint, textX[i], getCellCenter(i, 0).x, 0f - border / 2, true)
                drawTextCentred(canvas, coordinatesPaint, textX[i], getCellCenter(i, 0).x, fullLength + border / 2, true)
            }
            for (i in this.boardSize downTo 1) {
                drawTextCentred(canvas, coordinatesPaint, textY[i-1], 0f - border / 2, getCellCenter(0, this.boardSize - i).y)
                drawTextCentred(canvas, coordinatesPaint, textY[i-1], fullLength + border / 2, getCellCenter(0, this.boardSize - i).y)
            }
        }
    }

    private fun drawStones(canvas: Canvas, position: Position) {
        for (p in position.allStonesCoordinates) {
            val type = position.getStoneAt(p.x, p.y)

            val center = getCellCenter(p.x, p.y)
            //canvas.drawCircle(center.x, center.y, cellSize / 2f - stoneSpacing, territoryPaint);
            shadowDrawable.setBounds(
                    (center.x - cellSize / 2f + stoneSpacing - cellSize / 20f).toInt(),
                    (center.y - cellSize / 2f + stoneSpacing - cellSize / 20f).toInt(),
                    (center.x + cellSize / 2f - stoneSpacing + cellSize / 12f).toInt(),
                    (center.y + cellSize / 2f - stoneSpacing + cellSize / 9f).toInt()
            )
            if (!(fadeOutRemovedStones && position.removedSpots.contains(p))) {
                shadowDrawable.draw(canvas)
            }

            val drawable = if (type == StoneType.BLACK) blackStoneDrawable else whiteStoneDrawable
            drawable.alpha = if (fadeOutRemovedStones && position.removedSpots.contains(p)) 100 else 255
            drawable.setBounds(
                    (center.x - cellSize / 2f + stoneSpacing).toInt(),
                    (center.y - cellSize / 2f + stoneSpacing).toInt(),
                    (center.x + cellSize / 2f - stoneSpacing).toInt(),
                    (center.y + cellSize / 2f - stoneSpacing).toInt()
            )
            drawable.draw(canvas)
        }
    }

    /**
     * Draw decorations, such as the last move indicator. In the future,
     * the position object might contain other decorations, such as dead
     * stone or territory indicators.
     * @param canvas
     */
    private fun drawDecorations(canvas: Canvas, position: Position) {
        if(drawLastMove) {
            position.lastMove?.let {

                val type = position.lastPlayerToMove
                val center = getCellCenter(it.x, it.y)

                decorationsPaint.color = if (type == StoneType.WHITE) Color.BLACK else Color.WHITE
                canvas.drawCircle(center.x, center.y, (cellSize / 4).toFloat(), decorationsPaint)
            }
        }

        position.variation.forEachIndexed { i, p ->
            val center = getCellCenter(p.x, p.y)
            val stone = position.getStoneAt(p)
            if(stone == null || stone == StoneType.WHITE) {
                textPaint.color = Color.BLACK
            } else {
                textPaint.color = Color.WHITE
            }
            drawTextCentred(canvas, textPaint, (i+1).toString(), center.x, center.y)
        }
    }

    private fun drawBackground(canvas: Canvas) {
        //canvas.drawARGB(255, 0xDE, 0xB0, 0x66);
        //        canvas.drawBitmap(texture, 0, 0, null);
        texture?.let {
            val src = Rect(0, 0, it.width, it.height)
            val dest = Rect(0, 0, width, height)
            canvas.drawBitmap(texture, src, dest, null)
        } ?: Log.e("BoardView", "Null background!!!")
    }

    private fun getCellCenter(i: Int, j: Int): PointF {
        val center = PointF()
        val halfCell = cellSize / 2f
        center.set(i * cellSize + halfCell, j * cellSize + halfCell)
        if (FUZZY_PLACEMENT) {
            // TODO: don't instantiate here
            val r = Random((i * 100 + j).toLong())
            center.offset(
                    r.nextFloat() * 3f * stoneSpacing - 1.5f * stoneSpacing,
                    r.nextFloat() * 3f * stoneSpacing - 1.5f * stoneSpacing
            )
        }

        return center
    }

    fun tapUpObservable(): Observable<Point> = tapUpSubject.hide()

    private var lastHotTrackedPoint: Point? = null
    fun tapMoveObservable(): Observable<Point> = tapMoveSubject.filter {
        it != lastHotTrackedPoint
    }.doOnNext { lastHotTrackedPoint = it }

    fun showCandidateMove(candidateMove: Point?, candidate: StoneType?) {
        this.candidateMove = candidateMove
        this.candidateType = candidate
        invalidate()
    }

    fun refreshBoardView() {
        //dummy onSizeChanged call to trigger the border re-computation
        onSizeChanged(width, height, width, height)
        invalidate()
    }

    companion object {
        private val FUZZY_PLACEMENT = false
        private var texture: Bitmap? = null
    }
}
