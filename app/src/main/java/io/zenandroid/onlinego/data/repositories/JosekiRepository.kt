package io.zenandroid.onlinego.data.repositories

import android.util.Log
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.model.Mark
import io.zenandroid.onlinego.data.model.ogs.JosekiPosition
import io.zenandroid.onlinego.data.model.ogs.PlayCategory
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.utils.recordException

class JosekiRepository(
        private val restService: OGSRestService,
        private val dao: GameDao
) {

    private val disposable = CompositeDisposable()
    private val customMarkPattern = "<(.):([A-H]|[J-T]\\d{1,2})>".toPattern()
    private val headerWithMissingSpaceRegex = "#(?!\\s|#)".toRegex()
    private val positionLink = "<position: (\\d+)>".toRegex()

    fun getJosekiPosition(id: Long?): Flowable<JosekiPosition> {
        disposable += restService.getJosekiPositions(id)
                .subscribe(this::savePositionsToDB, this::onError)

        val dbObservable =
                if(id == null) dao.getJosekiRootPosition()
                else dao.getJosekiPostion(id)

        return dbObservable
                .map(this::extractLabelsFromDescription)
                .doOnNext {
                    it.next_moves = dao.getChildrenPositions(it.node_id ?: 0).map(this::extractLabelsFromDescription)
                }
                .distinctUntilChanged()
    }

    private fun savePositionsToDB(list: List<JosekiPosition>) {
        val children = mutableListOf<JosekiPosition>()
        list.forEach { pos ->
            pos.next_moves?.forEach {
                it.parent_id = pos.node_id
            }
            val isRoot = pos.play == ".root"
            pos.parent_id = if(isRoot) null else pos.parent?.node_id

            pos.next_moves?.let {
                children += pos
            }
        }
        dao.insertJosekiPositionsWithChildren(list, children)
    }

    private fun onError(error: Throwable) {
        Log.e("JosekiRepository", error.message, error)
        recordException(error)
    }

    private fun extractLabelsFromDescription(originalPos: JosekiPosition): JosekiPosition {
        var newDescription: String? = null
        originalPos.description?.let {
            newDescription = it.replace(headerWithMissingSpaceRegex, "# ")
            val matcher = customMarkPattern.matcher(newDescription!!)
            val sb = StringBuffer()
            val labels = mutableListOf<Mark>()
            while(matcher.find()) {
                val label = matcher.group(1)
                val coordinate = matcher.group(2)!!

                labels.add(Mark(RulesManager.coordinateToCell(coordinate), label, PlayCategory.LABEL))

                matcher.appendReplacement(sb, "**$label**")
            }
            originalPos.labels = labels
            matcher.appendTail(sb)
            newDescription = sb.toString().replace(positionLink) { match: MatchResult ->
                "[Position ${match.groupValues[1]}](${match.groupValues[1]})"
            }
        }

        originalPos.description = newDescription
        return originalPos
    }
}