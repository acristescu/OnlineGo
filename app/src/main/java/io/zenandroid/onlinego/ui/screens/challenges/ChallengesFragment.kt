package io.zenandroid.onlinego.ui.screens.challenges

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.utils.showIf
import io.zenandroid.onlinego.data.model.ogs.SeekGraphChallenge
import kotlinx.android.synthetic.main.fragment_challenges.*
import org.koin.android.ext.android.get


/**
 * Created by alex on 05/11/2017.
 */
@Deprecated("Obsolete")
class ChallengesFragment : Fragment(), ChallengesContract.View {

    private lateinit var presenter: ChallengesContract.Presenter
    private lateinit var adapter: ArrayAdapter<SeekGraphChallenge>
    private var analytics = OnlineGoApplication.instance.analytics

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_challenges, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1)
//        challengesRecycler.layoutManager = LinearLayoutManager(context)
        challengesRecycler.adapter = adapter

        presenter = ChallengesPresenter(this, get(), get())
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

    override fun addChallenge(challenge: SeekGraphChallenge) {
        adapter.add(challenge)
    }

    override fun setOverlayVisibility(visibility: Boolean) {
        overlay.showIf(visibility)
    }

    override fun removeChallenge(challenge: SeekGraphChallenge) {
        adapter.remove(challenge)
    }

    override fun removeAllChallenges() {
        adapter.clear()
    }
}