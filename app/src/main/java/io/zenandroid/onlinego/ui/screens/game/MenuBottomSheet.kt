package io.zenandroid.onlinego.ui.screens.game

import android.content.Context
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import io.zenandroid.onlinego.R

class MenuBottomSheet(
        context: Context,
        private val options: List<GameContract.MenuItem>,
        private val onSelect: (GameContract.MenuItem) -> Unit
) : BottomSheetDialog(context) {

    private val groupAdapter = GroupAdapter<GroupieViewHolder>()

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.dialog_game_menu, null)
        setContentView(view)

        findViewById<RecyclerView>(R.id.menuRecycler)?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter
        }
        groupAdapter.setOnItemClickListener { item, _ ->
            dismiss()
            onSelect((item as MenuRecyclerItem).item)
         }
        groupAdapter.update(options.map(::MenuRecyclerItem))
    }
}