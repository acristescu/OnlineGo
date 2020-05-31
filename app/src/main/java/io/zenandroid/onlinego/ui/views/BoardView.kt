package io.zenandroid.onlinego.ui.views

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.*
import androidx.core.content.res.ResourcesCompat
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.ogs.PlayCategory
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import org.koin.core.context.KoinContextHandler
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
    var boardSize = 19
        set(boardSize) {
            field = boardSize
            computeDimensions(width)
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
    var drawCoordinates = false
        set(value) {
            field = value
            computeDimensions(width)
            invalidate()
        }
    var drawMarks = false
        set(value) {
            field = value
            invalidate()
        }
    var fadeOutRemovedStones = false
        set(value) {
            field = value
            invalidate()
        }
    var drawShadow = true
        set(value) {
            field = value
            invalidate()
        }

    private val coordinatesX = arrayOf("A","B","C","D","E","F","G","H","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z")
    private val coordinatesY = (1..25).map(Int::toString)

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

    private val whiteStoneDrawable = ResourcesCompat.getDrawable(resources, R.mipmap.stone_white, null)!!
    private val blackStoneDrawable = ResourcesCompat.getDrawable(resources, R.mipmap.stone_black, null)!!
    private val shadowDrawable = ResourcesCompat.getDrawable(resources, R.drawable.gradient_shadow, null)!!

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
        linesPaint.apply {
            strokeWidth = 2f
            color = -0x33cce7f0
            strokeCap = Paint.Cap.ROUND
        }

        linesHighlightPaint.apply {
            strokeWidth = 4f
            color = resources.getColor(R.color.white)
            strokeCap = Paint.Cap.ROUND
        }

        decorationsPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        territoryPaint.strokeWidth = 4f

        preloadResources(resources)
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
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val size = if (widthMode == MeasureSpec.EXACTLY && widthSize > 0) {
            widthSize
        } else if (heightMode == MeasureSpec.EXACTLY && heightSize > 0) {
            heightSize
        } else {
            if (widthSize < heightSize) widthSize else heightSize
        }

        val finalMeasureSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        super.onMeasure(finalMeasureSpec, finalMeasureSpec)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        computeDimensions(w)
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

    private fun computeDimensions(w: Int) {
        val usableWidth = if (drawCoordinates) {
            w - (w / this.boardSize.toFloat() * 1f).roundToInt()
        } else w

        cellSize = usableWidth / this.boardSize
        linesPaint.strokeWidth = (cellSize / 35f).coerceAtMost(2f)
        linesHighlightPaint.strokeWidth = linesPaint.strokeWidth * 2
        decorationsPaint.strokeWidth = cellSize / 20f
        stoneSpacing = cellSize / 35f
        border = ((w - this.boardSize * cellSize) / 2).toFloat()
        textPaint.textSize = cellSize.toFloat() * .65f
        coordinatesPaint.textSize = cellSize.toFloat() * .4f
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
        if (drawCoordinates) {
            val fullLength = (cellSize * this.boardSize).toFloat()
            for (i in 0 until this.boardSize) {
                drawTextCentred(canvas, coordinatesPaint, coordinatesX[i], getCellCenter(i, 0).x, 0f - border / 2, true)
                drawTextCentred(canvas, coordinatesPaint, coordinatesX[i], getCellCenter(i, 0).x, fullLength + border / 2, true)
            }
            for (i in this.boardSize downTo 1) {
                drawTextCentred(canvas, coordinatesPaint, coordinatesY[i-1], 0f - border / 2, getCellCenter(0, this.boardSize - i).y)
                drawTextCentred(canvas, coordinatesPaint, coordinatesY[i-1], fullLength + border / 2, getCellCenter(0, this.boardSize - i).y)
            }
        }
    }

    private fun drawStones(canvas: Canvas, position: Position) {
        for (p in position.allStonesCoordinates) {
            val type = position.getStoneAt(p.x, p.y)

            val center = getCellCenter(p.x, p.y)
            //canvas.drawCircle(center.x, center.y, cellSize / 2f - stoneSpacing, territoryPaint);
            val isFadedOut = fadeOutRemovedStones && position.removedSpots.contains(p)
            if (drawShadow && !isFadedOut) {
                shadowDrawable.setBounds(
                        (center.x - cellSize / 2f + stoneSpacing - cellSize / 20f).toInt(),
                        (center.y - cellSize / 2f + stoneSpacing - cellSize / 20f).toInt(),
                        (center.x + cellSize / 2f - stoneSpacing + cellSize / 12f).toInt(),
                        (center.y + cellSize / 2f - stoneSpacing + cellSize / 9f).toInt()
                )
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
            decorationsPaint.style = Paint.Style.STROKE
            position.lastMove?.let {

                val type = position.lastPlayerToMove
                val center = getCellCenter(it.x, it.y)

                decorationsPaint.color = if (type == StoneType.WHITE) Color.BLACK else Color.WHITE
                canvas.drawCircle(center.x, center.y, cellSize / 4f, decorationsPaint)
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

        if(drawMarks) {
            decorationsPaint.style = Paint.Style.FILL_AND_STROKE
            position.customMarks.forEach {
                val center = getCellCenter(it.placement.x, it.placement.y)
                decorationsPaint.color = determineMarkColor(it)
                canvas.drawCircle(center.x, center.y, cellSize / 2f - stoneSpacing, decorationsPaint)
                it.text?.let {
                    drawTextCentred(canvas, textPaint, it, center.x, center.y)
                }
            }
        }
    }

    private fun drawBackground(canvas: Canvas) {
        texture?.let {
            val src = Rect(0, 0, it.width, it.height)
            val dest = Rect(0, 0, width, height)
            canvas.drawBitmap(it, src, dest, null)
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

    private fun determineMarkColor(mark: Position.Mark) =
        when(mark.category) {
            PlayCategory.IDEAL -> 0xC0D0FFC0.toInt()
            PlayCategory.GOOD -> 0xC0F0FF90.toInt()
            PlayCategory.MISTAKE -> 0xC0ED7861.toInt()
            PlayCategory.TRICK -> 0xC0D384F9.toInt()
            PlayCategory.QUESTION -> 0xC079B3E4.toInt()
            PlayCategory.LABEL -> 0x70FFFFFF.toInt()
            null -> Color.GRAY
        }

    companion object {
        private val FUZZY_PLACEMENT = false
        private var texture: Bitmap? = null

        @Synchronized
        fun preloadResources(resources: Resources, forceTextureReload: Boolean = false) {
            if (texture == null || forceTextureReload) {
                val settingsRepository: SettingsRepository = KoinContextHandler.get().get()
                texture = when (settingsRepository.appTheme) {
                    "Light" -> BitmapFactory.decodeResource(resources, R.drawable.texture_light)
                    "Dark" -> BitmapFactory.decodeResource(resources, R.drawable.texture_dark)
                    else -> BitmapFactory.decodeResource(resources, R.drawable.texture)
                }
            }
        }
    }
}
