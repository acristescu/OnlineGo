package io.zenandroid.onlinego.ui.views

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.os.bundleOf
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.OnlineGoApplication
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.Game
import io.zenandroid.onlinego.data.repositories.ActiveGamesRepository
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.databinding.ViewGameNotificationsButtonBinding
import io.zenandroid.onlinego.ui.screens.game.GAME_ID
import io.zenandroid.onlinego.ui.screens.game.GAME_SIZE
import io.zenandroid.onlinego.utils.NotificationUtils
import io.zenandroid.onlinego.utils.addToDisposable
import org.koin.core.context.GlobalContext.get

class GameNotificationsButton: FrameLayout {

    private val binding: ViewGameNotificationsButtonBinding

    private val analytics = OnlineGoApplication.instance.analytics
    private val activeGameRepository: ActiveGamesRepository = get().get()
    private val settingsRepository: SettingsRepository = get().get()

    private var lastGameNotified: Game? = null
    private var lastMoveCount: Int? = null
    private val subscriptions = CompositeDisposable()

    private var notificationsButtonEnabled: Boolean
        get() = binding.notificationsButton.isEnabled
        set(value) {
            binding.notificationsButton.isEnabled = value
            val targetAlpha = if(value) 1f else .33f
            binding.notificationsButton.animate().alpha(targetAlpha)
        }
    private var notificationsBadgeVisible: Boolean
        get() { return binding.badge.alpha == 1f }
        set(value) { binding.badge.animate().alpha(if(value) 1f else 0f) }

    private var notificationsBadgeCount: String? = null
        set(value) { binding.badge.text = value }


    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = ViewGameNotificationsButtonBinding.inflate(inflater, this, true)
        binding.notificationsButton.setOnClickListener { onNotificationClicked() }
    }

    private fun onNotificationClicked() {
        analytics.logEvent("my_move_clicked", null)
        val gamesList = activeGameRepository.myTurnGamesList
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
        navigateToGameScreen(gameToNavigate)
    }

    private fun navigateToGameScreen(game: Game) {
        findNavController()
            .navigate(
                R.id.gameFragment,
                bundleOf(GAME_ID to game.id, GAME_SIZE to game.width),
                NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .build()
            )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        activeGameRepository.myMoveCountObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::onMyMoveCountChanged)
            .addToDisposable(subscriptions)
    }

    private fun onMyMoveCountChanged(myMoveCount: Int) {
        if (myMoveCount == 0) {
            notificationsButtonEnabled = false
            notificationsBadgeVisible = false
            NotificationUtils.cancelNotification(context)
        } else {
            notificationsButtonEnabled = true
            notificationsBadgeVisible = true
            notificationsBadgeCount = myMoveCount.toString()
            lastMoveCount?.let {
                if(myMoveCount > it) {
                    vibrate()
                }
            }
        }
        lastMoveCount = myMoveCount
    }

    override fun onDetachedFromWindow() {
        subscriptions.clear()
        super.onDetachedFromWindow()
    }

    private fun vibrate() {
        if(!settingsRepository.vibrate) {
            return
        }
        val v = getSystemService(context, Vibrator::class.java) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v.vibrate(20)
        }
    }
}