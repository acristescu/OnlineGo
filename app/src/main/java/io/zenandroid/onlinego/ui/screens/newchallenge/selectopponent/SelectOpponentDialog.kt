package io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.data.model.local.Player
import io.zenandroid.onlinego.ui.screens.newchallenge.selectopponent.searchplayer.SearchPlayerFragment

class SelectOpponentDialog : DialogFragment(), SelectBotFragment.OnOpponentSelected {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(Player::class.java)

    private val TABS = listOf(
            "Bot" to SelectBotFragment::class.java,
            "Recent" to SelectRecentFragment::class.java,
            "Search" to SearchPlayerFragment::class.java
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        isCancelable = true
        setStyle(STYLE_NORMAL, 0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return inflater.inflate(R.layout.dialog_select_opponent, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ViewPager2>(R.id.viewPager).adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = TABS.size

            override fun createFragment(position: Int): Fragment {
                return TABS[position].second.newInstance()
            }

        }
        TabLayoutMediator(view.findViewById(R.id.tabs), view.findViewById(R.id.viewPager), TabLayoutMediator.TabConfigurationStrategy { tab, position -> tab.text = TABS[position].first }).attach()
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.attributes?.let {
            it.width = ViewGroup.LayoutParams.MATCH_PARENT
            it.height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog?.window?.attributes = it
        }
    }

    override fun onOpponentSelected(opponent: Player) {
        val serialized = moshi.toJson(opponent)
        val intent = Intent().putExtra("OPPONENT", serialized)
        targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
        dismiss()
    }

}