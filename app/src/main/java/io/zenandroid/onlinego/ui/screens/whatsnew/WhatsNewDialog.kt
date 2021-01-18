package io.zenandroid.onlinego.ui.screens.whatsnew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.zenandroid.onlinego.R
import io.zenandroid.onlinego.utils.WhatsNewUtils

class WhatsNewDialog : DialogFragment() {

    private lateinit var markwon: Markwon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        isCancelable = true
        setStyle(STYLE_NORMAL, 0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return inflater.inflate(R.layout.dialog_whatsnew, container, false)
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.attributes?.let {
            it.width = ViewGroup.LayoutParams.MATCH_PARENT
            it.height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog?.window?.attributes = it
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        markwon = Markwon.builder(requireContext())
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        builder.headingBreakColor(0x00FF0000)
                    }
                })
                .build()

        view.findViewById<View>(R.id.dismissButton).setOnClickListener {
            dismiss()
        }

        markwon.setMarkdown(view.findViewById(R.id.text), WhatsNewUtils.whatsNewText)
    }

}