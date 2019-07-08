package io.zenandroid.onlinego.puzzle

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import io.zenandroid.onlinego.ogs.OGSServiceImpl

const val PUZZLE_ID = "PUZZLE_ID"

class PuzzleFragment: Fragment(), PuzzleContract.View {
    companion object {
        fun createFragment(puzzleId: Long) = PuzzleFragment().apply {
            arguments = Bundle().apply {
                putLong(PUZZLE_ID, puzzleId)
            }
        }
    }

    private lateinit var presenter: PuzzleContract.Presenter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        presenter = PuzzlePresenter(
                view = this,
                service = OGSServiceImpl,
                puzzleId = arguments!!.getLong(PUZZLE_ID)
        )
    }

    override fun onResume() {
        super.onResume()
        presenter.subscribe()
    }

    override fun onPause() {
        presenter.unsubscribe()
        super.onPause()
    }
}