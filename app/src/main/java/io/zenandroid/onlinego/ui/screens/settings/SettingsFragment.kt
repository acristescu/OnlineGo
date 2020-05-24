package io.zenandroid.onlinego.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.awesomedialog.blennersilva.awesomedialoglibrary.AwesomeInfoDialog
import com.google.firebase.analytics.FirebaseAnalytics
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.repositories.UserSessionRepository
import io.zenandroid.onlinego.ui.screens.login.LoginActivity
import org.koin.android.ext.android.inject

class SettingsFragment : PreferenceFragmentCompat() {

    private val userSessionRepository: UserSessionRepository by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings_notifications)
        addPreferencesFromResource(R.xml.settings)
        val themePreference = preferenceManager.findPreference("app_theme")
        if (themePreference != null) {
            themePreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                        var themeOption = newValue as String
                        when (themeOption) {
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
                        true
                    }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when(preference?.key) {
            "about" -> {
                AwesomeInfoDialog(context)
                        .setTitle("About")
                        .setMessage("MrAlex's OnlineGo client for OGS server. Version ${BuildConfig.VERSION_NAME}.")
                        .setDialogBodyBackgroundColor(R.color.colorOffWhite)
                        .setDialogIconAndColor(R.drawable.ic_dialog_info, R.color.whiteStones)
                        .setCancelable(true)
                        .setColoredCircle(R.color.colorPrimaryDark)
                        .setPositiveButtonText("OK")
                        .setPositiveButtonbackgroundColor(R.color.colorPrimaryDark)
                        .setPositiveButtonClick { }
                        .show()
                return true
            }
            "privacyPolicy" -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.freeprivacypolicy.com/privacy/view/40f932df1dfc9af1b04df367aa6f14f0")))
            }
            "logout" -> {
                AwesomeInfoDialog(context)
                        .setTitle("Log Out")
                        .setMessage("Are you sure you want to log out? You won't be able to use the app until you log back in")
                        .setDialogBodyBackgroundColor(R.color.colorOffWhite)
                        .setColoredCircle(R.color.colorPrimaryDark)
                        .setDialogIconAndColor(R.drawable.ic_dialog_info, R.color.whiteStones)
                        .setCancelable(true)
                        .setPositiveButtonText("Log out")
                        .setPositiveButtonbackgroundColor(R.color.colorPrimaryDark)
                        .setPositiveButtonTextColor(R.color.colorText)
                        .setNegativeButtonText("Cancel")
                        .setNegativeButtonbackgroundColor(R.color.colorPrimaryDark)
                        .setNegativeButtonTextColor(R.color.colorText)
                        .setPositiveButtonClick {
                            context?.let { FirebaseAnalytics.getInstance(it).logEvent("logout_clicked", null) }
                            userSessionRepository.logOut()
                            startActivity(Intent(context, LoginActivity::class.java))
                            activity?.finish()
                        }
                        .setNegativeButtonClick {}
                        .show()
            }
            "notification_advanced" -> {
                val intent = Intent()
                intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"

                //for Android 5-7
                intent.putExtra("app_package", activity?.packageName)
                intent.putExtra("app_uid", activity?.applicationInfo?.uid)

                // for Android O
                intent.putExtra("android.provider.extra.APP_PACKAGE", activity?.packageName)

                startActivity(intent)
            }
        }
        return super.onPreferenceTreeClick(preference)
    }
}