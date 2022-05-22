package io.zenandroid.onlinego.ui.screens.game_legacy.gameInfo

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.databinding.DialogGameInfoBinding
import io.zenandroid.onlinego.utils.timeControlDescription

class GameInfoDialog : DialogFragment() {

    private val groupAdapter = GroupAdapter<GroupieViewHolder>()
    private lateinit var binding: DialogGameInfoBinding

    lateinit var game: Game

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        isCancelable = true
        setStyle(STYLE_NORMAL, 0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogGameInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter
        }

        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.attributes?.let {
            it.width = ViewGroup.LayoutParams.MATCH_PARENT
            it.height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog?.window?.attributes = it
        }

        groupAdapter.clear()
        if(this::game.isInitialized) {
            game.name?.let { groupAdapter.add(GameInfoItem("Game Name", it)) }
            game.rules?.let { groupAdapter.add(GameInfoItem("Rules", it)) }
            game.handicap?.let { groupAdapter.add(GameInfoItem("Handicap", it.toString())) }
            game.ranked?.let { groupAdapter.add(GameInfoItem("Ranked", it.yesNo())) }
            game.disableAnalysis?.let {
                groupAdapter.add(
                    GameInfoItem(
                        "Analysis/Conditional moves",
                        (!it).enabledDisabled()
                    )
                )
            }
            game.timeControl?.let {
                groupAdapter.add(
                    GameInfoItem(
                        "Time controls",
                        timeControlDescription(it)
                    )
                )
            }
        } else {
            dismiss()
        }
    }

    private fun Boolean.yesNo() = if(this) "yes" else "no"
    private fun Boolean.enabledDisabled() = if(this) "Enabled" else "Disabled"
}