package io.zenandroid.onlinego.spectate

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.extensions.showIf
import io.zenandroid.onlinego.model.ogs.Challenge
import io.zenandroid.onlinego.ogs.OGSServiceImpl





/**
 * Created by alex on 05/11/2017.
 */
class ChallengesFragment : Fragment(), ChallengesContract.View {

    @BindView(R.id.challenges_recycler) lateinit var challengesRecycler: ListView
    @BindView(R.id.overlay) lateinit var overlay: View

    private lateinit var unbinder: Unbinder
    private lateinit var presenter: ChallengesContract.Presenter
    private lateinit var adapter: ArrayAdapter<Challenge>
    private var analytics = OnlineGoApplication.instance.analytics


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_challenges, container, false)
        unbinder = ButterKnife.bind(this, view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1)
//        challengesRecycler.layoutManager = LinearLayoutManager(context)
        challengesRecycler.adapter = adapter

        presenter = ChallengesPresenter(this, OGSServiceImpl.instance)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
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

    override fun addChallenge(challenge: Challenge) {
        adapter.add(challenge)
    }

    override fun setOverlayVisibility(visibility: Boolean) {
        overlay.showIf(visibility)
    }

    override fun removeChallenge(challenge: Challenge) {
        adapter.remove(challenge)
    }

    override fun removeAllChallenges() {
        adapter.clear()
    }
}