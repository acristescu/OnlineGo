package io.zenandroid.onlinego.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import com.awesomedialog.blennersilva.awesomedialoglibrary.AwesomeInfoDialog
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.R


class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when(preference?.key) {
            "about" -> {
                AwesomeInfoDialog(context)
                        .setTitle("About")
                        .setMessage("MrAlex's OnlineGo client for OGS server. Version ${BuildConfig.VERSION_NAME}.")
                        .setCancelable(true)
                        .setColoredCircle(R.color.colorPrimary)
                        .setPositiveButtonText("OK")
                        .setPositiveButtonbackgroundColor(R.color.colorPrimary)
                        .setPositiveButtonClick { }
                        .show()
                return true
            }
            "privacyPolicy" -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.freeprivacypolicy.com/privacy/view/40f932df1dfc9af1b04df367aa6f14f0")))
            }
        }
        return super.onPreferenceTreeClick(preference)
    }
}