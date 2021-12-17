package io.zenandroid.onlinego.ui.screens.newchallenge

import android.app.AlertDialog
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import io.zenandroid.onlinego.R
import it.sephiroth.android.library.numberpicker.*

class NameValuePicker : FrameLayout {

    var name: String? = null
        set(value) {
            field = value
            findViewById<TextView>(R.id.nameView).text = value
        }

    var icon: String? = null
        set(value) {
            field = value
            findViewById<TextView>(R.id.nameView).text = value
        }

    var value: Int = 0
        set(value) {
            field = value
            findViewById<NumberPicker>(R.id.pickerView).apply {
                setProgress(value)
            }
        }

    var valuesCallback: (() -> List<String?>)? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        val view = View.inflate(context, R.layout.view_name_value_picker, this)
        val subviews = ArrayList<View>()
        view.apply {
            findViewsWithText(subviews, value.toString(), View.FIND_VIEWS_WITH_TEXT)
        }
        subviews.filterIsInstance<EditText>().forEach { 
            val color = ResourcesCompat.getColor(this.resources, R.color.colorActionableText, null)
            it.setTextColor(color)
        }
        findViewById<NumberPicker>(R.id.pickerView).doOnProgressChanged { _, progress, _ ->
            this.value = progress
        }
    }

}
