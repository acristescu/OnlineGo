package io.zenandroid.onlinego.ui.screens.game

import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.ui.screens.game.GameContract.MenuItem.*
import kotlinx.android.synthetic.main.item_game_menu.*

class MenuRecyclerItem(val item: GameContract.MenuItem) : Item(item.hashCode().toLong()) {
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        when(item) {
            RESIGN -> {
                viewHolder.titleView.text = "Resign"
                viewHolder.iconView.setImageResource(R.drawable.ic_flag)
            }
            PASS -> {
                viewHolder.titleView.text = "Pass"
                viewHolder.iconView.setImageResource(R.drawable.ic_pass)
            }
            GAME_INFO -> {
                viewHolder.titleView.text = "Game Info"
                viewHolder.iconView.setImageResource(R.drawable.ic_dialog_info)
            }
            ESTIMATE_SCORE -> {
                viewHolder.titleView.text = "Estimate Score"
                viewHolder.iconView.setImageResource(R.drawable.ic_score)
            }
            ANALYZE -> {
                viewHolder.titleView.text = "Analyze"
                viewHolder.iconView.setImageResource(R.drawable.ic_thinking)
            }
            SHOW_COORDINATES -> {
                viewHolder.titleView.text = "Show Coordinates"
                viewHolder.iconView.setImageResource(R.drawable.ic_coordinates)
            }
            HIDE_COORDINATES -> {
                viewHolder.titleView.text = "Hide Coordinates"
                viewHolder.iconView.setImageResource(R.drawable.ic_coordinates)
            }
            DOWNLOAD_SGF -> {
                viewHolder.titleView.text = "Download as SGF"
                viewHolder.iconView.setImageResource(R.drawable.ic_save_black_24dp)
            }
            ACCEPT_UNDO -> {
                viewHolder.titleView.text = "Accept undo"
                viewHolder.iconView.setImageResource(R.drawable.ic_undo)
            }
            REQUEST_UNDO -> {
                viewHolder.titleView.text = "Request undo"
                viewHolder.iconView.setImageResource(R.drawable.ic_undo)
            }
            ABORT_GAME -> {
                viewHolder.titleView.text = "Abort Game"
                viewHolder.iconView.setImageResource(R.drawable.ic_cancel)
            }
            OPEN_IN_BROWSER -> {
                viewHolder.titleView.text = "Open in Browser"
                viewHolder.iconView.setImageResource(R.drawable.ic_link_black_24dp)
            }
        }.let {}
    }

    override fun getLayout() =
            R.layout.item_game_menu
}