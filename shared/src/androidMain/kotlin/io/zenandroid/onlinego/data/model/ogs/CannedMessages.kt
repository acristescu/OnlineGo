package io.zenandroid.onlinego.data.model.ogs

object CannedMessages {
  private val messages = mapOf(
    "warn_beginner_escaper" to """
            Hi, welcome to OGS!

            Please consider resigning games rather than letting them time out, as this is fairer to the other player than making them wait for your clock to run out. Thank you.
        """.trimIndent(),

    "warn_escaper" to """
            It has come to our attention that you abandoned game #{{param}} and allowed it to time out rather than resigning.

            Players are required to end their games properly, as letting them time out can force the other player to wait unnecessarily, and prevent them from moving on to the next game.

            Please ensure that you end your games properly by accepting the correct score immediately after passing, or by resigning if you feel the position is hopeless.

            This helps maintain a positive gaming environment for everyone involved.
        """.trimIndent(),

    "ack_educated_beginner_escaper" to """
            Thanks for the report about '{{param}}', we've asked your newcomer opponent to be more respectful of people's time.
        """.trimIndent(),

    "ack_educated_beginner_escaper_and_annul" to """
            Thanks for the report about '{{param}}', we've asked your newcomer opponent to be more respectful of people's time.

            That incorrectly scored game has been annulled.
        """.trimIndent(),

    "ack_warned_escaper" to """
            Thank you for your report, '{{param}}' has been given a formal warning about finishing games properly.
        """.trimIndent(),

    "ack_warned_escaper_and_annul" to """
            Thank you for your report, '{{param}}' has been given a formal warning about finishing games properly, and that abandoned game annulled.
        """.trimIndent(),

    "no_escaping_evident" to """
            Thank you for bringing the possible instance of '{{param}}' abandoning the game to our attention.

            We looked into the game and did not see them failing to finish the game properly.

            If a person has not started playing, it is OK for you to "Cancel" the game.

            It may be that you need to report a different type of problem, or provide more explanation - you are welcome to raise a new report if that is the case.

            Thank you for helping keep OGS enjoyable for everyone. We appreciate it.
        """.trimIndent(),

    "not_escaping_cancel" to """
            Thank you for bringing the possible instance of '{{param}}' abandoning the game to our attention.

            We looked into the game and see that they used "Cancel".

            Players are allowed to "Cancel" a game during the first moves.

            If you think the person is abusing this feature, please file a report with more details.

            Thank you for helping keep OGS enjoyable for everyone. We appreciate it.
        """.trimIndent(),

    "warn_first_turn_escaper" to """
            We've noticed that you joined game #{{param}} but didn't make any moves.

            It's possible you didn't notice this game start - we understand that.

            However, could you please take care avoid this situation, so that other users are not left waiting and wondering.

            Thank you for helping keep OGS enjoyable for everyone. We appreciate it.
        """.trimIndent(),

    "notify_warned_first_turn_escaper" to """
            We've noticed that the other player left game #{{param}} without making any moves.

            We've automatically alerted that person about this, and asked them to be more respectful of people's time - you don't need to report it.

            Hopefully they'll be more careful next time!
        """.trimIndent(),

    "warn_beginner_staller" to """
            Hi, welcome to OGS!

            It appears that you delayed the end of game #{{param}}, which can frustrate the other player and prevent them from moving on to the next game.

            Since you are a new player, no action will be taken against your account. We simply ask that you learn when to end a game.

            Until you develop the experience to judge better, if the other player passes and there are no open borders between your stones then you should also pass.

            After passing, promptly accept the correct score.

            If in doubt about this sort of situation. please ask for help in chat or the forums.
        """.trimIndent(),

    "warn_staller" to """
            It has come to our attention that you delayed the end of game #{{param}}, which can frustrate the other player and prevent them from moving on to their next game.

            Players are required to end their games properly, as letting them time out can cause the other player to wait unnecessarily, and prevent them from moving on to the next game.

            Please ensure that you end your games properly by accepting the correct score immediately after passing, or by resigning if you feel the position is hopeless.

            This helps maintain a positive gaming environment for everyone involved.
        """.trimIndent(),

    "ack_educated_beginner_staller" to """
            Thanks for the report about '{{param}}', we've asked your newcomer opponent to be more respectful of people's time.
        """.trimIndent(),

    "ack_educated_beginner_staller_and_annul" to """
            Thanks for the report about '{{param}}', we've asked your newcomer opponent to be more respectful of people's time.

            That incorrectly scored game has been annulled.
        """.trimIndent(),

    "ack_warned_staller" to """
            Thank you for your report, '{{param}}' has been given a formal warning about finishing games properly.
        """.trimIndent(),

    "ack_warned_staller_and_annul" to """
            Thank you for your report, '{{param}}' has been given a formal warning about finishing games properly, and that abandoned game annulled.
        """.trimIndent(),

    "no_stalling_evident" to """
            Thank you for bringing the possible instance of stalling play by '{{param}}' to our attention. We looked into the report and don't see evidence of stalling.

            Note that the correct way to signal the game has finished is to pass.  If you didn't pass, then the other player is entitled to keep playing.

            It may be that you need to report a different type of problem, or provide more explanation - you are welcome to raise a new report if that is the case.

            Thank you for helping keep OGS enjoyable for everyone. We appreciate it.
        """.trimIndent(),

    "warn_beginner_score_cheat" to """
            It appears that you delayed the end of game #{{param}}, by clicking on the board to change the score incorrectly.   This can frustrate the other player and prevent them from moving on to the next game.

            Since you are a new player, no action will be taken against your account. We simply ask that you learn when to end a game.

            Until you develop the experience to judge better, if the other player passes and there are no open borders between your stones then you should also pass.

            After passing, promptly accept the correct score.

            If in doubt about this sort of situation. please ask for help in chat or the forums.
        """.trimIndent(),

    "warn_score_cheat" to """
            We noticed that you incorrectly changed the score at the end of game #{{param}}.

            While this might be a genuine mistake, please review the game and be sure you understand the final score.

            In future, we hope that you will end your games properly by first closing all the borders of your territory and secondly by accepting the correct score immediately after passing.

            In case of a disagreement over what the correct score is, we ask you to contact a moderator.

            Unfortunately, some users use this form of score manipulation to cheat, if this happens repeatedly we'll have no alternative than to suspend your account.
        """.trimIndent(),

    "ack_educated_beginner_score_cheat" to """
            Thanks for the report about '{{param}}'.

            It seems that person was a complete beginner - we have tried to explain that games should be ended correctly, to pass when their opponent passes, and to accept promptly, trusting the auto-score.
        """.trimIndent(),

    "ack_educated_beginner_score_cheat_and_annul" to """
            Thanks for the report about '{{param}}'.

            It seems that person was a complete beginner - we have tried to explain that games should be ended correctly, to pass when the other player passes, and to accept promptly, trusting the auto-score.

            That incorrectly scored game has been annulled.
        """.trimIndent(),

    "ack_warned_score_cheat" to """
            Thank you for your report, '{{param}}' has been given a formal warning about scoring properly.
        """.trimIndent(),

    "ack_warned_score_cheat_and_annul" to """
            Thank you for your report, '{{param}}' has been given a formal warning about scoring properly, and that cheated game annulled.
        """.trimIndent(),

    "no_score_cheating_evident" to """
            Thank you for bringing the possible instance of score cheating by '{{param}}' to our attention. We looked into the report and couldn't see evidence of score cheating.

            It may be that you need to report a different type of problem, or provide more explanation - you are welcome to raise a new report if that is the case.

            Thank you for helping keep OGS enjoyable for everyone. We appreciate it.
        """.trimIndent(),

    "no_ai_use_evident" to """
            Thank you for bringing the possible instance of AI use in Game #{{param}} to our attention. We looked into the game and couldn't see evidence of AI use.   

            It may be that you need to provide more explanation - you are welcome to raise a new report if that is the case.

            Thank you for helping keep OGS enjoyable for everyone. We appreciate it.
        """.trimIndent(),

    "no_ai_use_bad_report" to """
            Thank you for bringing the possible instance of AI use in Game #{{param}} to our attention. We looked into the game and couldn't see evidence of AI use.   
            
            Your report did not contain any real evidence of AI use - we'd prefer if you could provide more details, so our time is not wasted guessing.
            
            If you have a good reason to suspect AI use, please take some time to provide a detailed report describing why.

            Thank you for helping keep OGS enjoyable for everyone. We appreciate it.
        """.trimIndent(),

    "annul_no_warning" to """
            Just a note to let you know that we've annulled game #{{param}}, as the outcome was wrong.

            No-one was at fault, but we felt it was the best way to resolve the situation.
        """.trimIndent(),

    "ack_annul_no_warning" to """
            Thanks for your report about #{{param}}.

            We annulled that game, as the outcome was wrong.

            No-one was at fault - we felt this was the best way to resolve the situation.
        """.trimIndent(),

    "final_warn_escaper" to """
            Important: this is a final warning.

            It seems you failed to end game #{{param}} properly, and let it time out.

            If you continue to abandon games without finishing them properly your account will be suspended.

            We have previous explained that you need to resign if you feel the position is hopeless, or pass and accept the correct score then the game is over. 

            Please take care to do that each time, and ask for help if you are not clear what is the problem.

            Thanks.
        """.trimIndent(),

    "final_warn_escaper_and_annul" to """
            Important: this is a final warning.

            It seems you failed to end game #{{param}} properly, and let it time out.

            The outcome was wrong as a result - we've annulled that game.

            If you continue to abandon games without finishing them properly your account will be suspended.

            We have previous explained that you need to resign if you feel the position is hopeless, or pass and accept the correct score then the game is over. 

            Please take care to do that each time, and ask for help if you are not clear what is the problem.

            Thanks.
        """.trimIndent(),

    "final_warn_staller" to """
            Important: this is a final warning.

            It seems you delayed the end of game #{{param}}, which can frustrate the other player and prevent them from moving on to the next game.

            If you continue to delay games without finishing them properly your account will be suspended.

            We've previously explained that you need to end games properly, by accepting the correct score immediately after passing, or by resigning if you feel the position is hopeless.

            Please take care to do that each time, and ask for help if you are not clear what is the problem.

            Thanks.
        """.trimIndent(),

    "final_warn_staller_and_annul" to """
            Important: this is a final warning.

            It seems you delayed the end of game #{{param}}, which can frustrate the other player and prevent them from moving on to the next game.

            The outcome was wrong as a result - we've annulled that game.

            If you continue to delay games without finishing them properly your account will be suspended.

            We've previously explained that you need to end games properly, by accepting the correct score immediately after passing, or by resigning if you feel the position is hopeless.

            Please take care to do that each time, and ask for help if you are not clear what is the problem.

            Thanks.
        """.trimIndent(),

    "final_warn_score_cheat" to """
            Important: this is a final warning.

            It seems you incorrectly changed the score at the end of game #{{param}}.

            If you continue to change the score incorrectly your account will be suspended.

            We've previously explained that you need to end games properly, by accepting the correct score immediately after passing, or by resigning if you feel the position is hopeless.

            Please take care to do that each time, and ask for help if you are not clear what is the problem.

            Thanks.
        """.trimIndent(),

    "final_warn_score_cheat_and_annul" to """
            Important: this is a final warning.

            It seems you incorrectly changed the score at the end of game #{{param}}.

            The outcome was wrong as a result - we've annulled that game.

            If you continue to change the score incorrectly your account will be suspended.

            We've previously explained that you need to end games properly, by accepting the correct score immediately after passing, or by resigning if you feel the position is hopeless.

            Please take care to do that each time, and ask for help if you are not clear what is the problem.

            Thanks.
        """.trimIndent(),

    "ack_final_warn_escaper" to """
            Thank you for your report.  '{{param}}' has been given a final warning about abandoning games.

            If this continues, their account will be suspended.
        """.trimIndent(),

    "ack_final_warn_escaper_and_annul" to """
            Thank you for your report.  '{{param}}' has been given a final warning about abandoning games.

            If this continues, their account will be suspended.

            That game has been annulled.
        """.trimIndent(),

    "ack_final_warn_staller" to """
            Thank you for your report.  '{{param}}' has been given a final warning about stalling.

            If this continues, their account will be suspended.
        """.trimIndent(),

    "ack_final_warn_staller_and_annul" to """
            Thank you for your report.  '{{param}}' has been given a final warning about stalling.

            If this continues, their account will be suspended.

            That game has been annulled.
        """.trimIndent(),

    "ack_final_warn_score_cheat" to """
            Thank you for your report.  '{{param}}' has been given a final warning about cheating the score.

            If this continues, their account will be suspended.
        """.trimIndent(),

    "ack_final_warn_score_cheat_and_annul" to """
            Thank you for your report.  '{{param}}' has been given a final warning about cheating the score.

            If this continues, their account will be suspended.

            That game has been annulled.
        """.trimIndent(),

    "ack_suspended" to """
            Thank you for your report.  '{{param}}' is a repeat offender, their account has been suspended.
        """.trimIndent(),

    "ack_suspended_and_annul" to """
            Thank you for your report.  '{{param}}' is a repeat offender, their has been suspended.

            The reported game has been annulled.
        """.trimIndent(),

    "warn_duplicate_report" to """
            Thanks for your additional report about '{{param}}'.

            Please don't file multiple reports for the same thing - that creates a lot of work for us tidying up, which could be time spent better on other reports.

            We appreciate hearing about problems, but one report is enough for each incident - more than that will slow us down.

            Thanks!
        """.trimIndent(),

    "report_type_changed" to """
            Thanks for your recent report.   We've had to change the 'report type':

                {{param}}.

            It makes it easier and quicker to process reports if they are raised with the correct type - if you could help with that we'd appreciate it.

            If this change seems wrong, we'd welcome feedback about that - please contact a moderator to let them know.
        """.trimIndent(),

    "bot_owner_notified" to """
            Thanks for your recent report about {{param}}.

            We've notified the owner of that bot.
        """.trimIndent()
  )

  fun convertCannedMessage(msgId: String, interpolationData: String?): String {
    val message = messages[msgId] ?: return msgId

    return  message.replace("{{param}}", interpolationData ?: "")
  }
}
