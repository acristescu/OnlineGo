package io.zenandroid.onlinego.utils
import java.security.MessageDigest

object WhatsNewUtils {

    val shouldDisplayDialog: Boolean
        get() = PersistenceManager.previousWhatsNewTextHashed != null && PersistenceManager.previousWhatsNewTextHashed != hashString(currentText)
    val whatsNewText = currentText

    fun textShown() {
        PersistenceManager.previousWhatsNewTextHashed = hashString(currentText)
    }

    private fun hashString(text: String): String {
        return MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8)).fold("", { str, it -> str + "%02x".format(it) })
    }
}

private const val currentText = """
## What's new

## Fixed bugs

- "What's new" dialog should no longer appear once dismissed (not until a new release/announcement)

## About project

This is an open-source project. To find out how you can contribute or support the project, head over to the project’s [Github page](https://github.com/acristescu/OnlineGo).
"""