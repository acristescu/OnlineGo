package io.zenandroid.onlinego.ogs

import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.model.Position
import io.zenandroid.onlinego.model.ogs.JosekiPosition
import io.zenandroid.onlinego.model.ogs.PlayCategory

object JosekiRepository {

    private val disposable = CompositeDisposable()
    private val customMarkPattern = "<(.):([A-H]|[J-T]\\d{1,2})>".toPattern()
    private val headerWithMissingSpaceRegex = "#(?!\\s|#)".toRegex()

    internal fun subscribe() {
    }

    fun unsubscribe() {
    }

    fun getJosekiPosition(id: Long?): Single<JosekiPosition> {
        return OGSServiceImpl.getJosekiPosition(id).map(this::processJosekiPosition)
    }

    private fun processJosekiPosition(originalPos: JosekiPosition): JosekiPosition {
        var newDescription: String? = null
        originalPos.description?.let {
            newDescription = it.replace(headerWithMissingSpaceRegex, "# ")
            val matcher = customMarkPattern.matcher(newDescription)
            val sb = StringBuffer()
            val labels = mutableListOf<Position.Mark>()
            while(matcher.find()) {
                val label = matcher.group(1)
                val coordinate = matcher.group(2)

                labels.add(Position.Mark(Position.coordinateToPoint(coordinate), label, PlayCategory.LABEL))

                matcher.appendReplacement(sb, "**$label**")
            }
            originalPos.labels = labels
            matcher.appendTail(sb)
            newDescription = sb.toString()

        }
        return originalPos.copy(
                description = newDescription
        )
    }
}