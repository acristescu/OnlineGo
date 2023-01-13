package io.zenandroid.onlinego.ui.screens.settings

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.italic
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.BoardTheme
import io.zenandroid.onlinego.data.repositories.SettingsRepository
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.databinding.FragmentSettingsBinding
import io.zenandroid.onlinego.ui.screens.main.MainActivity
import io.zenandroid.onlinego.ui.views.BoardView
import io.zenandroid.onlinego.utils.processGravatarURL
import org.koin.android.ext.android.inject


class SettingsFragment : Fragment() {

    private val userSessionRepository: UserSessionRepository by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val themes = arrayOf("System default", "Light", "Dark")
    private val boardThemes = BoardTheme.values()
    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            subscribeButton.setOnClickListener {
                view.findNavController().navigate(R.id.action_settingsFragment_to_supporterFragment)
            }

            aboutButton.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                        .setTitle("About")
                        .setMessage("MrAlex's OnlineGo client for OGS server. Version ${BuildConfig.VERSION_NAME}.")
                        .setBackground(ColorDrawable(resources.getColor(R.color.colorOffWhite)))
                        .setCancelable(true)
                        .setPositiveButton("OK") { _, _ -> }
                        .show()
            }
            privacyPolicyButton.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.freeprivacypolicy.com/privacy/view/40f932df1dfc9af1b04df367aa6f14f0")))
            }
            logoutButton.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Log Out")
                        .setMessage("Are you sure you want to log out? You won't be able to use the app until you log back in")
                        .setCancelable(true)
                        .setPositiveButton("Log out") { _, _ ->
                            context?.let { FirebaseAnalytics.getInstance(it).logEvent("logout_clicked", null) }
                            FirebaseCrashlytics.getInstance().sendUnsentReports()
                            userSessionRepository.logOut()
                            (activity as? MainActivity)?.showLogin()
                        }
                        .setNegativeButton("Cancel") { _, _ -> }
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

            soundButton.apply {
                isChecked = settingsRepository.sound
                setOnCheckedChangeListener { _, isChecked -> settingsRepository.sound = isChecked }
            }

            coordinates.apply {
                isChecked = settingsRepository.showCoordinates
                setOnCheckedChangeListener { _, isChecked -> settingsRepository.showCoordinates = isChecked }
            }

            ranks.apply {
                isChecked = settingsRepository.showRanks
                setOnCheckedChangeListener { _, isChecked -> settingsRepository.showRanks = isChecked }
            }

            // Light / Dark Theme
            val currentLightDarkTheme = settingsRepository.appTheme
            themeButton.apply {
                text = buildSpannedString {
                    append("Light/Dark Theme\n")
                    color(ContextCompat.getColor(requireContext(), R.color.colorTextSecondary)) {
                        italic { append(currentLightDarkTheme) }
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

            //Board Theme
            val currentBoardBackground = settingsRepository.boardTheme.name
            boardBackgroundTheme.apply {
                text = buildSpannedString {
                    append("Board theme\n")
                    color(ContextCompat.getColor(requireContext(), R.color.colorTextSecondary)) {
                        italic { append(currentBoardBackground) }
                    }
                }

                val adapter = BoardThemePreviewAdapter(boardThemes, context)

                setOnClickListener {
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Select board theme")
                        .setAdapter(adapter) { _, newBoardTheme ->
                            settingsRepository.boardTheme = BoardTheme.valueOf(boardThemes[newBoardTheme].name)
                            text = buildSpannedString {
                                append("Board theme\n")
                                color(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.colorTextSecondary
                                    )
                                ) {
                                    italic { append(boardThemes[newBoardTheme].displayName) }
                                }
                            }
                        }
                        .show()
                }
            }

            userSessionRepository.uiConfig?.user?.username?.let { name.text = it }
            iconView.doOnLayout {
                userSessionRepository.uiConfig?.user?.icon?.let {
                    Glide.with(this@SettingsFragment)
                            .load(processGravatarURL(it, iconView.width))
                            .transition(DrawableTransitionOptions.withCrossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()))
                            .apply(RequestOptions().centerCrop().placeholder(R.drawable.ic_person_outline))
                            .apply(RequestOptions().circleCrop().diskCacheStrategy(DiskCacheStrategy.RESOURCE))
                            .into(iconView)
                }
            }
        }
    }
}