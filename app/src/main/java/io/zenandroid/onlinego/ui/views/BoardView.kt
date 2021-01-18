package io.zenandroid.onlinego.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import androidx.core.content.res.ResourcesCompat
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.ogs.PlayCategory
import io.zenandroid.onlinego.gamelogic.Util
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt


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

    var animationEnabled = true
    private var fadeInAnimationAlpha = 0
    private var fadeOutAnimationAlpha = 0

    var position: Position? = null
        set(value) {
            if(field != value) {
                if(value != null && field != null && field?.hasTheSameStonesAs(value) == false) {
                    val newStones = value.stones.filter { field?.stones?.containsKey(it.key) == false || field?.stones?.get(it.key) != it.value }
                    if(newStones.isNotEmpty()) {
                        ValueAnimator.ofInt(100, 255).apply {
                            duration = 100
                            addUpdateListener {
                                fadeInAnimationAlpha = it.animatedValue as Int
                                invalidate()
                            }
                            start()
                        }
                    }
                    val disappearingStones = field?.stones?.filter { !value.stones.containsKey(it.key) } ?: emptyMap()
                    if(disappearingStones.isNotEmpty()) {
                        ValueAnimator.ofInt(255, 0).apply {
                            duration = 100
                            addUpdateListener {
                                fadeOutAnimationAlpha = it.animatedValue as Int
                                invalidate()
                            }
                            start()
                        }
                    }
                    stonesToFadeIn = newStones
                    stonesToFadeOut = disappearingStones
                }
                invalidate()
            }
            field = value
        }
    var isInteractive = false
    var drawLastMove = true
        set(value) {
            if(field != value) {
                invalidate()
            }
            field = value
        }
    var drawTerritory = false
        set(value) {
            if(field != value) {
                invalidate()
            }
            field = value
        }
    var drawCoordinates = false
        set(value) {
            if(field != value) {
                field = value
                computeDimensions(width)
                invalidate()
            }
        }
    var drawMarks = false
        set(value) {
            if(field != value) {
                invalidate()
            }
            field = value
        }
    var fadeOutRemovedStones = false
        set(value) {
            if(field != value) {
                invalidate()
            }
            field = value
        }
    var drawShadow = true
        set(value) {
            if(field != value) {
                invalidate()
            }
            field = value
        }
    var drawAiEstimatedOwnership = false
        set(value) {
            if(field != value) {
                invalidate()
            }
            field = value
        }
    var drawHints = false
        set(value) {
            if(field != value) {
                invalidate()
            }
            field = value
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
    private var stonesToFadeIn: Map<Point, StoneType> = mapOf()
    private var stonesToFadeOut: Map<Point, StoneType> = mapOf()

    var onTapMove: ((Point) -> Unit)? = null
    var onTapUp: ((Point) -> Unit)? = null

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

        textPaint.apply {
            setShadowLayer(6.dp.value, 0f, 0f, Color.WHITE)
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
        if(eventCoords != lastHotTrackedPoint) {
            onTapMove?.invoke(eventCoords)
        }

        if (event.action == MotionEvent.ACTION_UP) {
            tapUpSubject.onNext(eventCoords)
            onTapUp?.invoke(eventCoords)
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
            drawAiEstimatedOwnership(canvas, it)
            drawHints(canvas, it)
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

    private fun drawAiEstimatedOwnership(canvas: Canvas, position: Position) {
        val ownershipMatrix =
                position.aiAnalysisResult?.ownership ?:
                position.aiQuickEstimation?.ownership

        if(drawAiEstimatedOwnership && ownershipMatrix?.isNotEmpty() == true) {
            val radius = cellSize / 4f
            for (i in 0 until boardSize) {
                for(j in 0 until boardSize) {
                    val ownership = ownershipMatrix[i*boardSize + j] // a float between -1 and 1, -1 is 100% solid black territory, 1 is 100% solid white territory
                    val alpha = (255 * abs(ownership)).toInt()

                    //
                    // Note: Dark grey instead of pure black as the black is perceived
                    // much stronger than the white on the light background giving the
                    // impression that black is always winning.
                    //
                    val colorWithoutAlpha = if(ownership > 0) Color.WHITE else Color.DKGRAY
                    val color = ColorUtils.setAlphaComponent(colorWithoutAlpha, alpha)
                    val center = getCellCenter(j, i)
                    if(
                            !(position.getStoneAt(j, i) == StoneType.WHITE && ownership > 0) &&
                            !(position.getStoneAt(j, i) == StoneType.BLACK && ownership < 0)
                    ) {
                        territoryPaint.color = color
                        canvas.drawCircle(center.x, center.y, radius, territoryPaint)
                    }
                }
            }
        }

    }

    private fun drawHints(canvas: Canvas, position: Position) {
        val hints = position.aiAnalysisResult?.moveInfos
        if(drawHints && hints != null) {
            for((index, hint) in hints.take(5).withIndex()) {
                val coords = Util.getCoordinatesFromGTP(hint.move, position.boardSize)
                val center = getCellCenter(coords.x, coords.y)
                val drawable = if (position.nextToMove == StoneType.BLACK) blackStoneDrawable else whiteStoneDrawable
                drawable.alpha = 100
                drawable.setBounds(
                        (center.x - cellSize / 2f + stoneSpacing).toInt(),
                        (center.y - cellSize / 2f + stoneSpacing).toInt(),
                        (center.x + cellSize / 2f - stoneSpacing).toInt(),
                        (center.y + cellSize / 2f - stoneSpacing).toInt()
                )
                drawable.draw(canvas)

                textPaint.color = if (position.nextToMove == StoneType.WHITE) Color.BLACK else Color.WHITE
                drawTextCentred(canvas, textPaint, (index + 1).toString(), center.x, center.y)
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
        for (item in stonesToFadeOut) {
            drawStone(canvas, position, item.key, item.value)
        }
        for (p in position.allStonesCoordinates) {
            val type = position.getStoneAt(p.x, p.y)
            drawStone(canvas, position, p, type)
        }
    }

    private fun drawStone(canvas: Canvas, position: Position, p: Point, type: StoneType?) {
        val center = getCellCenter(p.x, p.y)
        val alpha = when {
            fadeOutRemovedStones && position.removedSpots.contains(p) -> 100
            animationEnabled && stonesToFadeIn[p] == type -> fadeInAnimationAlpha
            animationEnabled && stonesToFadeOut[p] == type -> fadeOutAnimationAlpha
            else -> 255
        }
        val isFadedOut = fadeOutRemovedStones && position.removedSpots.contains(p)
        if (drawShadow && !isFadedOut) {
            shadowDrawable.alpha = if(alpha == 255) 255 else ((alpha / 255f) * (alpha / 255f) * 255f).toInt()
            shadowDrawable.setBounds(
                    (center.x - cellSize / 2f + stoneSpacing - cellSize / 20f).toInt(),
                    (center.y - cellSize / 2f + stoneSpacing - cellSize / 20f).toInt(),
                    (center.x + cellSize / 2f - stoneSpacing + cellSize / 12f).toInt(),
                    (center.y + cellSize / 2f - stoneSpacing + cellSize / 9f).toInt()
            )
            shadowDrawable.draw(canvas)
        }

        val drawable = if (type == StoneType.BLACK) blackStoneDrawable else whiteStoneDrawable
        drawable.alpha = alpha
        drawable.setBounds(
                (center.x - cellSize / 2f + stoneSpacing).toInt(),
                (center.y - cellSize / 2f + stoneSpacing).toInt(),
                (center.x + cellSize / 2f - stoneSpacing).toInt(),
                (center.y + cellSize / 2f - stoneSpacing).toInt()
        )
        drawable.draw(canvas)
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
                if(it.x != -1) {
                    val type = position.lastPlayerToMove
                    val center = getCellCenter(it.x, it.y)

                    decorationsPaint.color = if (type == StoneType.WHITE) Color.BLACK else Color.WHITE
                    canvas.drawCircle(center.x, center.y, cellSize / 4f, decorationsPaint)
                }
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
            position.customMarks.forEach {
                when(it.text) {
                    "#" -> { // last move mark
                        val type = position.getStoneAt(it.placement)
                        val center = getCellCenter(it.placement.x, it.placement.y)

                        decorationsPaint.style = Paint.Style.STROKE
                        decorationsPaint.color = if (type == StoneType.WHITE) Color.BLACK else Color.WHITE
                        canvas.drawCircle(center.x, center.y, cellSize / 4f, decorationsPaint)
                    }
                    else -> {
                        decorationsPaint.style = Paint.Style.FILL_AND_STROKE
                        decorationsPaint.color = determineMarkColor(it)
                        val center = getCellCenter(it.placement.x, it.placement.y)
                        canvas.drawCircle(center.x, center.y, cellSize / 2f - stoneSpacing, decorationsPaint)
                        it.text?.let {
                            drawTextCentred(canvas, textPaint, it, center.x, center.y)
                        }
                    }
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
            null -> 0x00FFFFFF.toInt()
        }

    companion object {
        private val FUZZY_PLACEMENT = false
        private var texture: Bitmap? = null

        @Synchronized
        fun unloadResources() {
            texture = null
        }

        @Synchronized
        fun preloadResources(resources: Resources) {
            if (texture == null) {
                texture = BitmapFactory.decodeResource(resources, R.mipmap.texture)
            }
        }
    }
}
