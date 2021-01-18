package io.zenandroid.onlinego.ui.screens.game

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.databinding.ItemGameMenuBinding
import io.zenandroid.onlinego.ui.screens.game.GameContract.MenuItem.*

class MenuRecyclerItem(val item: GameContract.MenuItem) : BindableItem<ItemGameMenuBinding>(item.hashCode().toLong()) {
    override fun bind(binding: ItemGameMenuBinding, position: Int) {
        when(item) {
            RESIGN -> {
                binding.titleView.text = "Resign"
                binding.iconView.setImageResource(R.drawable.ic_flag)
            }
            PASS -> {
                binding.titleView.text = "Pass"
                binding.iconView.setImageResource(R.drawable.ic_pass)
            }
            GAME_INFO -> {
                binding.titleView.text = "Game Info"
                binding.iconView.setImageResource(R.drawable.ic_dialog_info)
            }
            ESTIMATE_SCORE -> {
                binding.titleView.text = "Estimate Score"
                binding.iconView.setImageResource(R.drawable.ic_territory)
            }
            ANALYZE -> {
                binding.titleView.text = "Analyze"
                binding.iconView.setImageResource(R.drawable.ic_thinking)
            }
            SHOW_COORDINATES -> {
                binding.titleView.text = "Show Coordinates"
                binding.iconView.setImageResource(R.drawable.ic_coordinates)
            }
            HIDE_COORDINATES -> {
                binding.titleView.text = "Hide Coordinates"
                binding.iconView.setImageResource(R.drawable.ic_coordinates)
            }
            DOWNLOAD_SGF -> {
                binding.titleView.text = "Download as SGF"
                binding.iconView.setImageResource(R.drawable.ic_save_black_24dp)
            }
            ACCEPT_UNDO -> {
                binding.titleView.text = "Accept undo"
                binding.iconView.setImageResource(R.drawable.ic_undo)
            }
            REQUEST_UNDO -> {
                binding.titleView.text = "Request undo"
                binding.iconView.setImageResource(R.drawable.ic_undo)
            }
            ABORT_GAME -> {
                binding.titleView.text = "Abort Game"
                binding.iconView.setImageResource(R.drawable.ic_cancel)
            }
            OPEN_IN_BROWSER -> {
                binding.titleView.text = "Open in Browser"
                binding.iconView.setImageResource(R.drawable.ic_chrome)
            }
        }.let {}
    }

    override fun getLayout() =
            R.layout.item_game_menu

    override fun initializeViewBinding(view: View): ItemGameMenuBinding = ItemGameMenuBinding.bind(view)
}