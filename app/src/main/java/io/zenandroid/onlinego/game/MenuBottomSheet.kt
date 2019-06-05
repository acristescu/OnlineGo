package io.zenandroid.onlinego.game

import android.content.Context
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import io.zenandroid.onlinego.R
import kotlinx.android.synthetic.main.dialog_game_menu.*

class MenuBottomSheet(
        context: Context,
        private val options: List<GameContract.MenuItem>,
        private val onSelect: (GameContract.MenuItem) -> Unit
) : BottomSheetDialog(context) {

    private val adapter = GroupAdapter<ViewHolder>()

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.dialog_game_menu, null)
        setContentView(view)

        menuRecycler.layoutManager = LinearLayoutManager(context)
        menuRecycler.adapter = adapter
        adapter.setOnItemClickListener { item, _ ->
            dismiss()
            onSelect((item as MenuRecyclerItem).item)
         }
        adapter.update(options.map(::MenuRecyclerItem))
    }
}