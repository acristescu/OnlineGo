package io.zenandroid.onlinego.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.annotation.RequiresApi
import com.github.mikephil.charting.charts.BubbleChart
import com.github.mikephil.charting.components.MarkerView
import java.time.Instant.now

class ClickableBubbleChart : BubbleChart {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

	override fun onTouchEvent(event: MotionEvent): Boolean {
        // if there is no marker view or drawing marker is disabled
        val markerView = this.marker as? ClickableMarkerView
        return if (markerView != null && isDrawMarkersEnabled && valuesToHighlight()) {
            val rect = Rect(markerView.drawingPosX.toInt(), markerView.drawingPosY.toInt(),
                markerView.drawingPosX.toInt() + markerView.width, markerView.drawingPosY.toInt() + markerView.height)
            if (rect.contains(event.x.toInt(), event.y.toInt())) {
                // touch on marker -> dispatch touch event in to marker
                event.offsetLocation(-markerView.drawingPosX, -markerView.drawingPosY)
                markerView.dispatchTouchEvent(event)
                true
            } else {
                super.onTouchEvent(event)
            }
        } else {
            super.onTouchEvent(event)
        }
    }
}

abstract class ClickableMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {
    var drawingPosX: Float = 0f
    var drawingPosY: Float = 0f
    private val MAX_CLICK_DURATION = 500
    private var startClickTime: Long = 0

    abstract fun onClick(event: MotionEvent): Boolean

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.action) {
            MotionEvent.ACTION_DOWN -> {
                startClickTime = now().toEpochMilli()
            }
            MotionEvent.ACTION_UP -> {
                val clickDuration = now().toEpochMilli() - startClickTime
                if(clickDuration < MAX_CLICK_DURATION) {
                    return onClick(event)
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun draw(canvas: Canvas, posX: Float, posY: Float) {
        super.draw(canvas, posX, posY)
        val offset = getOffsetForDrawingAtPoint(posX, posY)
        this.drawingPosX = posX + offset.x
        this.drawingPosY = posY + offset.y
    }
}
