package io.zenandroid.onlinego.stats

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R

class StatsFragment : Fragment(), StatsContract.View {

    private lateinit var presenter: StatsContract.Presenter
    private var analytics = OnlineGoApplication.instance.analytics

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        presenter = StatsPresenter(this, analytics)
    }

    override fun onResume() {
        super.onResume()
        analytics.setCurrentScreen(activity!!, javaClass.simpleName, javaClass.simpleName)
        presenter.subscribe()
    }

    override fun onPause() {
        super.onPause()
        presenter.unsubscribe()
    }


}