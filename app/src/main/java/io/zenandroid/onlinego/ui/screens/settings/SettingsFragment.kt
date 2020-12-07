package io.zenandroid.onlinego.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.italic
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import com.awesomedialog.blennersilva.awesomedialoglibrary.AwesomeInfoDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.screens.login.LoginActivity
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.ui.views.BoardView
import io.zenandroid.onlinego.utils.processGravatarURL
import kotlinx.android.synthetic.main.fragment_settings.*
import org.koin.android.ext.android.inject

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val userSessionRepository: UserSessionRepository by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val themes = arrayOf("System default", "Light", "Dark")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeButton.setOnClickListener {
            (requireActivity() as MainActivity).navigateToSupporterScreen()
        }

        aboutButton.setOnClickListener {
            AwesomeInfoDialog(context)
                    .setTitle("About")
                    .setMessage("MrAlex's OnlineGo client for OGS server. Version ${BuildConfig.VERSION_NAME}.")
                    .setDialogBodyBackgroundColor(R.color.colorOffWhite)
                    .setDialogIconAndColor(R.drawable.ic_dialog_info, R.color.white)
                    .setCancelable(true)
                    .setColoredCircle(R.color.colorPrimary)
                    .setPositiveButtonText("OK")
                    .setPositiveButtonbackgroundColor(R.color.colorPrimaryDark)
                    .setPositiveButtonTextColor(R.color.white)
                    .setPositiveButtonClick { }
                    .show()
        }
        privacyPolicyButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.freeprivacypolicy.com/privacy/view/40f932df1dfc9af1b04df367aa6f14f0")))
        }
        logoutButton.setOnClickListener {
            AwesomeInfoDialog(context)
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out? You won't be able to use the app until you log back in")
                    .setDialogBodyBackgroundColor(R.color.colorOffWhite)
                    .setColoredCircle(R.color.colorPrimary)
                    .setDialogIconAndColor(R.drawable.ic_dialog_info, R.color.white)
                    .setCancelable(true)
                    .setPositiveButtonText("Log out")
                    .setPositiveButtonbackgroundColor(R.color.colorPrimaryDark)
                    .setPositiveButtonTextColor(R.color.white)
                    .setNegativeButtonText("Cancel")
                    .setNegativeButtonbackgroundColor(R.color.colorPrimaryDark)
                    .setNegativeButtonTextColor(R.color.white)
                    .setPositiveButtonClick {
                        context?.let { FirebaseAnalytics.getInstance(it).logEvent("logout_clicked", null) }
                        userSessionRepository.logOut()
                        startActivity(Intent(context, LoginActivity::class.java))
                        activity?.finish()
                    }
                    .setNegativeButtonClick {}
                    .show()
        }
        notificationsButton.setOnClickListener {
            val intent = Intent()
            intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"

            //for Android 5-7
            intent.putExtra("app_package", activity?.packageName)
            intent.putExtra("app_uid", activity?.applicationInfo?.uid)

            // for Android O
            intent.putExtra("android.provider.extra.APP_PACKAGE", activity?.packageName)

            startActivity(intent)
        }

        vibrateButton.apply {
            isChecked = settingsRepository.vibrate
            setOnCheckedChangeListener { _, isChecked -> settingsRepository.vibrate = isChecked }
        }

        coordinates.apply {
            isChecked = settingsRepository.showCoordinates
            setOnCheckedChangeListener { _, isChecked -> settingsRepository.showCoordinates = isChecked }
        }

        ranks.apply {
            isChecked = settingsRepository.showRanks
            setOnCheckedChangeListener { _, isChecked -> settingsRepository.showRanks = isChecked }
        }

        val currentTheme = settingsRepository.appTheme
        themeButton.apply {
            text = buildSpannedString {
                append("Theme\n")
                color(ContextCompat.getColor(requireContext(), R.color.colorTextSecondary)) {
                    italic { append(currentTheme) }
                }
            }

            setOnClickListener {
                MaterialAlertDialogBuilder(context)
                        .setTitle("Select theme")
                        .setItems(themes) { _, newTheme ->
                            settingsRepository.appTheme = themes[newTheme]
                            when (themes[newTheme]) {
                                "Light" -> {
                                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                                }
                                "Dark" -> {
                                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                                }
                                else -> {
                                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                                    } else {
                                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                                    }
                                }
                            }
                            BoardView.unloadResources()

                            text = buildSpannedString {
                                append("Theme\n")
                                color(ContextCompat.getColor(requireContext(), R.color.colorTextSecondary)) {
                                    italic { append(themes[newTheme]) }
                                }
                            }
                        }
                        .show()
            }
        }

        userSessionRepository.uiConfig?.user?.username?.let { name.text = it }
        iconView.doOnLayout {
            userSessionRepository.uiConfig?.user?.icon?.let {
                Glide.with(this)
                        .load(processGravatarURL(it, iconView.width))
                        .transition(DrawableTransitionOptions.withCrossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()))
                        .apply(RequestOptions().centerCrop().placeholder(R.drawable.ic_person_outline))
                        .apply(RequestOptions().circleCrop().diskCacheStrategy(DiskCacheStrategy.RESOURCE))
                        .into(iconView)
            }
        }
    }
}