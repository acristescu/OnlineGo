package io.zenandroid.onlinego.game.gameInfo

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.model.local.Game
import io.zenandroid.onlinego.utils.timeControlDescription
import kotlinx.android.synthetic.main.dialog_game_info.*

class GameInfoDialog : DialogFragment() {

    private val groupAdapter = GroupAdapter<ViewHolder>()

    lateinit var game: Game

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        isCancelable = true
        setStyle(STYLE_NORMAL, 0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return inflater.inflate(R.layout.dialog_game_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter
        }

        closeButton.setOnClickListener {
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
        game.name?.let { groupAdapter.add(GameInfoItem("Game Name", it)) }
        game.rules?.let { groupAdapter.add(GameInfoItem("Rules", it)) }
        game.ranked?.let { groupAdapter.add(GameInfoItem("Ranked", it.yesNo())) }
        game.disableAnalysis?.let { groupAdapter.add(GameInfoItem("Analysis/Conditional moves", (!it).enabledDisabled()))}
        game.timeControl?.let { groupAdapter.add(GameInfoItem("Time controls", timeControlDescription(it)))}
    }

    private fun Boolean.yesNo() = if(this) "yes" else "no"
    private fun Boolean.enabledDisabled() = if(this) "Enabled" else "Disabled"
}