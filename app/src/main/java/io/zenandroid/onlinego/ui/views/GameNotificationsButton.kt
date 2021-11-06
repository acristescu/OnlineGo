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
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.get
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
import io.zenandroid.onlinego.ui.screens.game.GAME_HEIGHT
import io.zenandroid.onlinego.ui.screens.game.GAME_ID
import io.zenandroid.onlinego.ui.screens.game.GAME_WIDTH
import io.zenandroid.onlinego.utils.NotificationUtils
import io.zenandroid.onlinego.utils.addToDisposable
import io.zenandroid.onlinego.utils.hide
import io.zenandroid.onlinego.utils.show
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.context.GlobalContext.get

class GameNotificationsButton: FrameLayout {

    private val binding: ViewGameNotificationsButtonBinding

    private val analytics = OnlineGoApplication.instance.analytics
    private val settingsRepository: SettingsRepository = get().get()

    private var viewModel: GameNotificationsButtonViewModel? = null

    private var lastMoveCount: Int? = null


    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = ViewGameNotificationsButtonBinding.inflate(inflater, this, true)
        binding.notificationsButton.setOnClickListener { onNotificationClicked() }
    }

    private fun onNotificationClicked() {
        analytics.logEvent("my_move_clicked", null)
        viewModel?.onNotificationClicked()
    }

    private fun navigateToGameScreen(game: Game) {
        findNavController()
            .navigate(
                R.id.gameFragment,
                bundleOf(GAME_ID to game.id, GAME_WIDTH to game.width, GAME_HEIGHT to game.height),
                NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(R.id.gameFragment, true)
                    .build()
            )
    }

    private fun onMyMoveCountChanged(myMoveCount: Int) {
        if (myMoveCount == 0) {
            binding.notificationsButton.apply {
                isEnabled = false
                alpha = .33f
            }
            binding.badge.alpha = 0f
            NotificationUtils.cancelNotification()
        } else {
            binding.notificationsButton.apply {
                isEnabled = true
                alpha = 1f
            }
            binding.badge.apply {
                alpha = 1f
                text = myMoveCount.toString()
            }
            lastMoveCount?.let {
                if(myMoveCount > it) {
                    vibrate()
                }
            }
        }
        lastMoveCount = myMoveCount
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewModel = ViewModelProvider(FragmentManager.findFragment<Fragment>(this).requireActivity(), object:
            ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return GameNotificationsButtonViewModel(get().get()) as T
            }
        }).get()
        viewModel?.apply {
            gamesCount.value?.let { onMyMoveCountChanged(it) }
            gamesCount.observe(findViewTreeLifecycleOwner()!!, ::onMyMoveCountChanged)
            navigateToGame.observe(findViewTreeLifecycleOwner()!!, ::navigateToGameScreen)
        }
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