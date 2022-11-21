package io.zenandroid.onlinego.ui.screens.settings

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.BoardTheme

class CustomAdapter(bordThemes: Array<BoardTheme>, mContext: Context) :
    ArrayAdapter<BoardTheme?>(
        mContext, R.layout.row_item_settings_dialog_theme, bordThemes
    ) {
    // View lookup cache
    private class ViewHolder {
        var themePreview: ImageView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get the data item for this position
//        var convertView = convertView
        val boardTheme = getItem(position)
        // Check if an existing view is being reused, otherwise inflate the view
        val viewHolder: ViewHolder // view lookup cache stored in tag
        val result: View
        if (convertView == null) {
            viewHolder = ViewHolder()
            val inflater = LayoutInflater.from(context)
            result = inflater.inflate(R.layout.row_item_settings_dialog_theme, parent, false)
            viewHolder.themePreview =
                result.findViewById<View>(R.id.theme_preview) as ImageView

            // IMAGE
//            val backgroundImage = boardTheme!!.backgroundImage
            val backgroundImage = boardTheme?.backgroundImage
            if (backgroundImage != null) {
                viewHolder.themePreview!!.setImageResource(backgroundImage)
            }

            // COLOR
            val backgroundColor = boardTheme?.backgroundColor
            if (backgroundColor != null) {
                val color = Color(ContextCompat.getColor(context, backgroundColor))
                viewHolder.themePreview!!.setBackgroundColor(color.toArgb())
            }
            result.tag = viewHolder
        } else {
            result = convertView
        }

        // Return the completed view to render on screen
        return result
    }
}