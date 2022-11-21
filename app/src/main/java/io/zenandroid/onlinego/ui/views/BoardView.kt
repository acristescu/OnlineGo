package io.zenandroid.onlinego.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.*
import android.os.Build
import androidx.core.content.res.ResourcesCompat
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.Cell
import io.zenandroid.onlinego.data.model.Mark
import io.zenandroid.onlinego.data.model.Position
import io.zenandroid.onlinego.data.model.StoneType
import io.zenandroid.onlinego.data.model.katago.MoveInfo
import io.zenandroid.onlinego.data.model.ogs.PlayCategory
import io.zenandroid.onlinego.gamelogic.Util
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt


/**
 * Created by alex on 1/8/2015.
 * This is a view that is capable of displaying a GO board and a GO game position
 * that is passed to it via setPosition()
 */
class BoardView : View {
    var boardWidth = 19
        set(boardWidth) {
            field = boardWidth
            computeDimensions(width, height)
        }

    var boardHeight = 19
        set(boardHeight) {
            field = boardHeight
            computeDimensions(width, height)
        }

    var animationEnabled = true
    private var fadeInAnimationAlpha = 0
    private var fadeOutAnimationAlpha = 0

    var position: Position? = null
        set(value) {
            if(field != value) {
                if(animationEnabled && value != null && field != null && field?.hasTheSameStonesAs(value) == false) {
                    val newStones = field?.let { (value.whiteStones - it.whiteStones) + (value.blackStones - it.blackStones) } ?: emptySet()
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
                    val disappearingWhiteStones = field?.let { it.whiteStones - value.whiteStones } ?: emptySet()
                    val disappearingBlackStones = field?.let { it.blackStones - value.blackStones } ?: emptySet()
                    if(disappearingWhiteStones.isNotEmpty() || disappearingBlackStones.isNotEmpty()) {
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
                    stonesToFadeOutWhite = disappearingWhiteStones
                    stonesToFadeOutBlack = disappearingBlackStones
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
                computeDimensions(width, height)
                invalidate()
            }
        }
    var texture = BitmapFactory.decodeResource(resources, R.drawable.wood)
        set(value) {
            if(field != value) {
                field = value
                computeDimensions(width, height)
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
    var hints: List<MoveInfo>? = null
    var ownership: List<Float>? = null

    private val coordinatesX = arrayOf("A","B","C","D","E","F","G","H","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z")
    private val coordinatesY = (1..25).map(Int::toString)

    //
    // Size of border between edge and first line
    //
    private var border = 0f

    private var xOffsetForNonSquareBoard = 0f
    private var yOffsetForNonSquareBoard = 0f

    private var linesPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var linesHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val decorationsPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val coordinatesPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val territoryPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val whiteStoneDrawable = ResourcesCompat.getDrawable(resources, R.mipmap.stone_white, null)!!
    private val blackStoneDrawable = ResourcesCompat.getDrawable(resources, R.mipmap.stone_black, null)!!

    private lateinit var whiteStoneBitmap: Bitmap
    private lateinit var blackStoneBitmap: Bitmap
    private val shadowDrawable = ResourcesCompat.getDrawable(resources, R.drawable.gradient_shadow, null)!!

    private val textBounds = Rect()

    //
    // Size (in px) of the cell
    //
    private var cellSize: Float = 0f
    private var stoneSpacing = 0f
    private var candidateMove: Cell? = null
    private var candidateType: StoneType? = null

    private val tapUpSubject = PublishSubject.create<Cell>()
    private val tapMoveSubject = PublishSubject.create<Cell>()
    private var stonesToFadeIn: Set<Cell> = setOf()
    private var stonesToFadeOutWhite: Set<Cell> = setOf()
    private var stonesToFadeOutBlack: Set<Cell> = setOf()
    private val stonePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    var onTapMove: ((Cell) -> Unit)? = null
    var onTapUp: ((Cell) -> Unit)? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun darkMode() =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    private fun init(context: Context) {
        linesPaint.apply {
            strokeWidth = 2f
            color = if(darkMode()) 0xFF000000.toInt() else 0xCC331810.toInt()
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.FILL
        }

        stonePaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        linesHighlightPaint.apply {
            strokeWidth = 4f
            color = 0xFFFFFF
            strokeCap = Paint.Cap.ROUND
        }

        decorationsPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        textPaint.apply {
            isSubpixelText = true
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

    private fun screenToBoardCoordinates(x: Float, y: Float): Cell {
        return Cell(
                ((x - border - xOffsetForNonSquareBoard) / cellSize).coerceIn(0f, boardWidth - 1f).toInt(),
                ((y - border - yOffsetForNonSquareBoard) / cellSize).coerceIn(0f, boardHeight - 1f).toInt()
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

        val size = when {
            widthMode == MeasureSpec.EXACTLY && widthSize > 0 -> widthSize
            heightMode == MeasureSpec.EXACTLY && heightSize > 0 -> heightSize
            widthSize != 0 && heightSize != 0 && widthSize < heightSize -> widthSize
            widthSize != 0 && heightSize != 0 && widthSize >= heightSize -> heightSize
            widthSize == 0 -> heightSize
            else -> widthSize
        }

        val finalMeasureSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        super.onMeasure(finalMeasureSpec, finalMeasureSpec)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        computeDimensions(w, h)
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawBackground(canvas)
        canvas.translate(xOffsetForNonSquareBoard + border, yOffsetForNonSquareBoard + border)

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

    private fun computeDimensions(w: Int, h: Int) {
        val usableWidth = if (drawCoordinates) {
            w - (w / this.boardWidth.toFloat()).roundToInt()
        } else w

        val usableHeight = if (drawCoordinates) {
            h - (h / this.boardHeight.toFloat()).roundToInt()
        } else h

        cellSize = minOf(usableWidth.toFloat() / this.boardWidth, usableHeight.toFloat() / this.boardHeight)
        linesPaint.strokeWidth = (cellSize / 35f).coerceAtMost(2f)
        linesHighlightPaint.strokeWidth = linesPaint.strokeWidth * 2
        decorationsPaint.strokeWidth = cellSize / 20f
        stoneSpacing = (cellSize / 35f).coerceAtLeast(1f)
        border = minOf((w - this.boardWidth * cellSize) / 2, (h - this.boardHeight * cellSize) / 2)
        textPaint.textSize = cellSize * .65f
        coordinatesPaint.textSize = cellSize * .4f

        xOffsetForNonSquareBoard = ((boardHeight - boardWidth) * cellSize / 2).coerceAtLeast(0f)
        yOffsetForNonSquareBoard = ((boardWidth - boardHeight) * cellSize / 2).coerceAtLeast(0f)

        if(cellSize > 0) {
            whiteStoneBitmap = convertVectorIntoBitmap(R.drawable.ic_stone_white_svg, ceil(cellSize - 2 * stoneSpacing).toInt())
            blackStoneBitmap = convertVectorIntoBitmap(R.drawable.ic_stone_black_svg, ceil(cellSize - 2 * stoneSpacing).toInt())
        }

        shadowPaint.apply {
            color = 0x01FF0000
            style = Paint.Style.FILL
            setShadowLayer(
                    cellSize / 10f,
                    0f,
                    stoneSpacing * 2,
                    0xBB000000.toInt()
            )        }
    }

    private fun convertVectorIntoBitmap(vector: Int, width: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val vec = VectorDrawableCompat.create(OnlineGoApplication.instance.resources, vector, null)!!
        vec.setBounds(0, 0, canvas.width, canvas.height)
        vec.draw(canvas)
        return bitmap
    }
    
    private fun drawTerritory(canvas: Canvas, position: Position) {
        if(!drawTerritory) {
            return
        }
        for(i in 0 until boardWidth) {
            for(j in 0 until boardHeight) {
                val p = Cell(i, j)
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
        ownership?.let { ownership ->
            if(drawAiEstimatedOwnership && ownership.isNotEmpty()) {
                val radius = cellSize / 4f
                for (i in 0 until boardWidth) {
                    for(j in 0 until boardHeight) {
                        val value = ownership[i*boardWidth + j] // a float between -1 and 1, -1 is 100% solid black territory, 1 is 100% solid white territory
                        val alpha = (255 * abs(value)).toInt()

                        //
                        // Note: Dark grey instead of pure black as the black is perceived
                        // much stronger than the white on the light background giving the
                        // impression that black is always winning.
                        //
                        val colorWithoutAlpha = if(value > 0) Color.WHITE else Color.DKGRAY
                        val color = ColorUtils.setAlphaComponent(colorWithoutAlpha, alpha)
                        val center = getCellCenter(j, i)
                        if(
                                !(position.whiteStones.contains(Cell(j, i)) && value > 0) &&
                                !(position.blackStones.contains(Cell(j, i)) && value < 0)
                        ) {
                            territoryPaint.color = color
                            canvas.drawCircle(center.x, center.y, radius, territoryPaint)
                        }
                    }
                }
            }
        }
    }

    private fun drawHints(canvas: Canvas, position: Position) {
        hints?.let {
            for((index, hint) in it.take(5).withIndex()) {
                val coords = Util.getCoordinatesFromGTP(hint.move, position.boardHeight)
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

    private fun drawSelection(canvas: Canvas, candidateMove: Cell) {
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
        if(boardWidth == boardHeight) {
            when (boardWidth) {
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
    }

    private fun drawSingleStarPoint(canvas: Canvas, i: Int, j: Int) {
        val center = getCellCenter(i, j)
        canvas.drawCircle(center.x, center.y, (cellSize / 15f).coerceAtLeast(2f), linesPaint)
    }

    private fun drawGrid(canvas: Canvas) {
        for (i in 0 until boardWidth) {
            val halfCell = cellSize / 2f
            val start = i * cellSize + halfCell
            val fullLength = (cellSize * this.boardHeight).toFloat()

            canvas.drawLine(start, halfCell, start, fullLength - halfCell, linesPaint)
            if((i == candidateMove?.x)) {
                canvas.drawLine(start, halfCell, start, fullLength - halfCell, linesHighlightPaint)
            }
        }
        for (i in 0 until boardHeight) {
            val halfCell = cellSize / 2f
            val start = i * cellSize + halfCell
            val fullLength = cellSize * this.boardWidth

            canvas.drawLine(halfCell, start, fullLength - halfCell, start, linesPaint)
            if(i == candidateMove?.y) {
                canvas.drawLine(halfCell, start, fullLength - halfCell, start, linesHighlightPaint)
            }
        }
    }

    private fun drawCoordinates(canvas: Canvas) {
        if (drawCoordinates) {
            for (i in 0 until this.boardWidth) {
                drawTextCentred(canvas, coordinatesPaint, coordinatesX[i], getCellCenter(i, 0).x, 0f - border / 2, true)
                drawTextCentred(canvas, coordinatesPaint, coordinatesX[i], getCellCenter(i, 0).x, cellSize * this.boardHeight + border / 2, true)
            }
            for (i in this.boardHeight downTo 1) {
                drawTextCentred(canvas, coordinatesPaint, coordinatesY[i-1], 0f - border / 2, getCellCenter(0, this.boardHeight - i).y)
                drawTextCentred(canvas, coordinatesPaint, coordinatesY[i-1], cellSize * this.boardWidth + border / 2, getCellCenter(0, this.boardHeight - i).y)
            }
        }
    }

    private fun drawStones(canvas: Canvas, position: Position) {
        for (item in stonesToFadeOutWhite) {
            drawStone(canvas, position, item, StoneType.WHITE)
        }
        for (item in stonesToFadeOutBlack) {
            drawStone(canvas, position, item, StoneType.BLACK)
        }

        position.whiteStones.forEach { drawStone(canvas, position, it, StoneType.WHITE) }
        position.blackStones.forEach { drawStone(canvas, position, it, StoneType.BLACK) }
    }

    private fun drawStone(canvas: Canvas, position: Position, p: Cell, type: StoneType?) {
        val center = getCellCenter(p.x, p.y)
        val alpha = when {
            fadeOutRemovedStones && position.removedSpots.contains(p) -> 100
            animationEnabled && stonesToFadeIn.contains(p) -> fadeInAnimationAlpha
            animationEnabled && (stonesToFadeOutWhite.contains(p) || stonesToFadeOutBlack.contains(p)) -> fadeOutAnimationAlpha
            else -> 255
        }
        val isFadedOut = fadeOutRemovedStones && position.removedSpots.contains(p)
        if (drawShadow && alpha == 255) {
            if(Build.VERSION.SDK_INT >= 28) {
                canvas.drawCircle(center.x, center.y, cellSize / 2f - stoneSpacing - 1.5f, shadowPaint)
            } else {
                shadowDrawable.alpha = if(alpha == 255) 255 else ((alpha / 255f) * (alpha / 255f) * 255f).toInt()
                shadowDrawable.setBounds(
                        (center.x - cellSize / 2f + stoneSpacing - cellSize / 20f).toInt(),
                        (center.y - cellSize / 2f + stoneSpacing - cellSize / 20f).toInt(),
                        (center.x + cellSize / 2f - stoneSpacing + cellSize / 12f).toInt(),
                        (center.y + cellSize / 2f - stoneSpacing + cellSize / 9f).toInt()
                )
                shadowDrawable.draw(canvas)
            }
        }

        stonePaint.alpha = alpha
        if(cellSize > 30) {
            val bitmap = if (type == StoneType.BLACK) blackStoneBitmap else whiteStoneBitmap
            canvas.drawBitmap(bitmap, center.x - cellSize / 2f + stoneSpacing, center.y - cellSize / 2f + stoneSpacing, stonePaint)
        } else {
            stonePaint.color = if (type == StoneType.BLACK) Color.BLACK else Color.WHITE
            stonePaint.style = Paint.Style.FILL
            canvas.drawCircle(center.x, center.y, cellSize / 2f - stoneSpacing, stonePaint)
            if(type == StoneType.WHITE) {
                stonePaint.color = 0xCC331810.toInt()
                stonePaint.style = Paint.Style.STROKE
                canvas.drawCircle(center.x, center.y, cellSize / 2f - stoneSpacing, stonePaint)
            }
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

    fun tapUpObservable(): Observable<Cell> = tapUpSubject.hide()

    private var lastHotTrackedPoint: Cell? = null
    fun tapMoveObservable(): Observable<Cell> = tapMoveSubject.filter {
        it != lastHotTrackedPoint
    }.doOnNext { lastHotTrackedPoint = it }

    fun showCandidateMove(candidateMove: Cell?, candidate: StoneType?) {
        this.candidateMove = candidateMove
        this.candidateType = candidate
        invalidate()
    }

    private fun determineMarkColor(mark: Mark) =
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
                texture = BitmapFactory.decodeResource(resources, R.drawable.wood)
            }
        }
    }
}
