package io.zenandroid.onlinego.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import com.awesomedialog.blennersilva.awesomedialoglibrary.AwesomeInfoDialog
import io.zenandroid.onlinego.BuildConfig
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.login.LoginActivity
import io.zenandroid.onlinego.ogs.OGSServiceImpl


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
            "logout" -> {
                AwesomeInfoDialog(context)
                        .setTitle("Log Out")
                        .setMessage("Are you sure you want to log out? You won't be able to use the app until you log back in")
                        .setColoredCircle(R.color.colorPrimary)
                        .setDialogIconAndColor(R.drawable.ic_dialog_info, R.color.white)
                        .setCancelable(true)
                        .setPositiveButtonText("Log out")
                        .setPositiveButtonbackgroundColor(R.color.colorPrimary)
                        .setPositiveButtonTextColor(R.color.white)
                        .setNegativeButtonText("Cancel")
                        .setNegativeButtonbackgroundColor(R.color.colorPrimary)
                        .setNegativeButtonTextColor(R.color.white)
                        .setPositiveButtonClick {
                            OGSServiceImpl.logOut()
                            startActivity(Intent(context, LoginActivity::class.java))
                            activity?.finish()
                        }
                        .setNegativeButtonClick {}
                        .show()
            }
        }
        return super.onPreferenceTreeClick(preference)
    }
}