package io.zenandroid.onlinego.ui.views

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.repositories.ActiveGamesRepository
import io.zenandroid.onlinego.utils.SingleLiveEvent
import io.zenandroid.onlinego.utils.addToDisposable

class GameNotificationsButtonViewModel(
    private val activeGamesRepository: ActiveGamesRepository
) : ViewModel() {

    val gamesCount: MutableLiveData<Int> = MutableLiveData(0)
    val navigateToGame: SingleLiveEvent<Game> = SingleLiveEvent()

    private val subscriptions = CompositeDisposable()
    private var lastGameNotified: Game? = null

    init {
        Log.d("***", "instantiated")
        activeGamesRepository.myMoveCountObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::onMyMoveCountChanged)
            .addToDisposable(subscriptions)
    }

    override fun onCleared() {
        subscriptions.clear()
        super.onCleared()
    }

    private fun onMyMoveCountChanged(myMoveCount: Int) {
        gamesCount.value = myMoveCount
    }

    fun onNotificationClicked() {
        val gamesList = activeGamesRepository.myTurnGamesList
        if(gamesList.isEmpty()) {
            FirebaseCrashlytics.getInstance().log("Notification clicked while no games available")
            return
        }
        val gameToNavigate = if(lastGameNotified == null) {
            gamesList[0]
        } else {
            val index = gamesList.indexOfFirst { it.id == lastGameNotified?.id }
            if(index == -1) {
                gamesList[0]
            } else {
                gamesList[(index + 1) % gamesList.size]
            }
        }
        lastGameNotified = gameToNavigate

        navigateToGame.value = gameToNavigate
    }

}