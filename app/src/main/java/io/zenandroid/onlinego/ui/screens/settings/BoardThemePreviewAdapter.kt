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

class BoardThemePreviewAdapter(bordThemes: Array<BoardTheme>, mContext: Context) :
    ArrayAdapter<BoardTheme?>(
        mContext, R.layout.row_item_settings_dialog_theme, bordThemes
    ) {
    // View lookup cache
    private class ViewHolder {
        var backGroundPreview: ImageView? = null
        var gridPreview: ImageView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val boardTheme = getItem(position)
        val viewHolder: ViewHolder // view lookup cache stored in tag
        val result: View
        if (convertView == null) {
            viewHolder = ViewHolder()
            val inflater = LayoutInflater.from(context)
            result = inflater.inflate(R.layout.row_item_settings_dialog_theme, parent, false)
            viewHolder.backGroundPreview =
                result.findViewById<View>(R.id.background) as ImageView
            viewHolder.gridPreview =
                result.findViewById<View>(R.id.grid) as ImageView

            // Background is image
            boardTheme?.backgroundImage?.let {
                viewHolder.backGroundPreview?.setImageResource(it)
                viewHolder.gridPreview?.setImageResource(boardTheme.gridPreview)
            }

            // Background is plain color
            boardTheme?.backgroundColor?.let {
                val color = Color(ContextCompat.getColor(context, it))
                viewHolder.backGroundPreview?.setBackgroundColor(color.toArgb())
                viewHolder.gridPreview?.setImageResource(boardTheme.gridPreview)
            }

            result.tag = viewHolder
        } else {
            result = convertView
        }

        // Return the completed view to render on screen
        return result
    }
}