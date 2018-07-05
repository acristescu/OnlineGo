package io.zenandroid.onlinego.learn

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import kotlinx.android.synthetic.main.fragment_learn.*


/**
 * Created by alex on 05/11/2017.
 */
class LearnFragment : Fragment(), LearnContract.View {

    private lateinit var presenter: LearnContract.Presenter
    private var analytics = OnlineGoApplication.instance.analytics
    private var groupAdapter = GroupAdapter<ViewHolder>().apply {
        add(LearnItem("Pro Games", "Database of pro games", R.drawable.ic_book, "BROWSE"))
        add(LearnItem("Live Games", "Online live games", R.drawable.ic_book, "WATCH"))
        add(LearnItem("Tsumego", "GO problems", R.drawable.ic_book, "SOLVE"))
        add(LearnItem("Tutorial", "GO rules and basics", R.drawable.ic_book, "LEARN"))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_learn, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        learnRecycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        learnRecycler.adapter = groupAdapter

        presenter = LearnPresenter(this, analytics)
    }

    override fun onResume() {
        super.onResume()
        analytics.setCurrentScreen(activity!!, javaClass.simpleName, null)
        presenter.subscribe()
    }

    override fun onPause() {
        super.onPause()
        presenter.unsubscribe()
    }

}