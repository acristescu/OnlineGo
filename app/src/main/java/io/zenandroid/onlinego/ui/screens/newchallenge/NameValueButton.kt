package io.zenandroid.onlinego.ui.screens.newchallenge

import android.app.AlertDialog
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import io.zenandroid.onlinego.R
import kotlinx.android.synthetic.main.view_name_value_button.view.*

class NameValueButton : FrameLayout {

    var name: String? = null
        set(value) {
            field = value
            nameView.text = value
        }

    var value: String = ""
        set(value) {
            field = value
            valueView.text = value
        }

    var valuesCallback: (() -> List<String?>)? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        val view = View.inflate(context, R.layout.view_name_value_button, this)
        setOnClickListener {
            valuesCallback?.invoke()?.let { list ->
                val array = list.toTypedArray()
                AlertDialog.Builder(context)
                        .setItems(array) { _, which ->
                            value = array[which] ?: ""
                        }
                        .setCancelable(true)
                        .create()
                        .show()
            }
        }
    }

}