package io.zenandroid.onlinego.learn

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.main.MainActivity
import kotlinx.android.synthetic.main.fragment_learn.*


/**
 * Created by alex on 05/11/2017.
 */
class LearnFragment : Fragment(), LearnContract.View {

    private lateinit var presenter: LearnContract.Presenter
    private val learningItems = listOf(
            LearnItem("Joseki", "Explore openings", R.drawable.ic_book, "EXPLORE"),
            LearnItem("Pro Games", "Database of pro games", R.drawable.ic_book, "BROWSE"),
            LearnItem("Live Games", "Online live games", R.drawable.ic_book, "WATCH"),
            LearnItem("Tsumego", "GO problems", R.drawable.ic_book, "SOLVE"),
            LearnItem("Tutorial", "GO rules and basics", R.drawable.ic_book, "LEARN")
    )
    private val analytics = OnlineGoApplication.instance.analytics
    private val groupAdapter = GroupAdapter<GroupieViewHolder>()
            .apply { learningItems.forEach (::add) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_learn, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        learnRecycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        learnRecycler.adapter = groupAdapter
        groupAdapter.setOnItemClickListener { item, _ ->
            if(item == learningItems[0]) {
                (activity as MainActivity).navigateToJosekiExplorer()
            } else {
                Toast.makeText(context, "Not implemented yet!", Toast.LENGTH_SHORT).show()
            }
        }

        presenter = LearnPresenter(this, analytics)
    }

    override fun onResume() {
        super.onResume()
        analytics.setCurrentScreen(requireActivity(), javaClass.simpleName, null)
        presenter.subscribe()
    }

    override fun onPause() {
        super.onPause()
        presenter.unsubscribe()
    }

}