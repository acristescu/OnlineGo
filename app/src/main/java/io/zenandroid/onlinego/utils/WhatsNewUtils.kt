package io.zenandroid.onlinego.utils

object WhatsNewUtils {

    val shouldDisplayDialog: Boolean
            get() = PersistenceManager.lastWhatsNewText != currentText
//            get() = PersistenceManager.lastWhatsNewText != null && PersistenceManager.lastWhatsNewText != currentText
    val whatsNewText = currentText

    fun textShown() {
        PersistenceManager.lastWhatsNewText = currentText
    }
}

private const val currentText = """
## What's new

- Dark mode. Head over to Settings to enable it. Thanks to Popz73 for the contribution!
- We now take into account the ping and server client clock differences to fine tune how much time there is left on the clock
- Paused games now correctly stop the timer ticking down, although there is still no indication the game is paused
- Absolute timers now work correctly
- The start timer (the time you have to make your first move) is now correctly shown

## Fixed bugs

- Finished games sometimes not showing up
- Opponent's pre-planned move now always shows correctly
- Fixed issues with accepting an opponent's undo request
- Added some UI “smoke tests” to the CI setup. This should greatly reduce the chances of a broken release

## About project

This is an open-source project. To find out how you can contribute or support the project, head over to the project’s [Github page](https://github.com/acristescu/OnlineGo).
"""