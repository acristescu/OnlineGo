package io.zenandroid.onlinego.settings

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
        if(preference?.key == "about") {
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
        return super.onPreferenceTreeClick(preference)
    }
}