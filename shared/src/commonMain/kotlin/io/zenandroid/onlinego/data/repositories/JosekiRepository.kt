package io.zenandroid.onlinego.data.repositories

import co.touchlab.kermit.Logger
import io.zenandroid.onlinego.data.db.GameDao
import io.zenandroid.onlinego.data.model.Mark
import io.zenandroid.onlinego.data.model.ogs.JosekiPosition
import io.zenandroid.onlinego.data.model.ogs.PlayCategory
import io.zenandroid.onlinego.data.ogs.OGSRestService
import io.zenandroid.onlinego.gamelogic.RulesManager
import io.zenandroid.onlinego.utils.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class JosekiRepository(
  private val restService: OGSRestService,
  private val dao: GameDao
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  fun getJosekiPosition(id: Long?): Flow<JosekiPosition> {
    scope.launch {
      try {
        val positions = restService.getJosekiPositions(id)
        savePositionsToDB(positions)
      } catch (e: Exception) {
        onError(e)
      }
    }
    val dbFlow =
      if (id == null) dao.getJosekiRootPosition()
      else dao.getJosekiPosition(id)
    return dbFlow
      .filterNotNull()
      .map(::extractLabelsFromDescription)
      .onEach {
        it.next_moves =
          dao.getChildrenPositions(it.node_id ?: 0).map(::extractLabelsFromDescription)
      }
      .distinctUntilChanged()
  }

  private suspend fun savePositionsToDB(list: List<JosekiPosition>) {
    val children = mutableListOf<JosekiPosition>()
    list.forEach { pos ->
      pos.next_moves?.forEach {
        it.parent_id = pos.node_id
      }
      val isRoot = pos.play == ".root"
      pos.parent_id = if (isRoot) null else pos.parent?.node_id
      pos.next_moves?.let {
        children += pos
      }
    }
    dao.insertJosekiPositionsWithChildren(list, children)
  }

  private fun onError(error: Throwable) {
    Logger.e(error.message.orEmpty(), error, "JosekiRepository")
    CrashReporter.recordException(error)
  }
}

private val customMarkRegex = "<(.):([A-H]|[J-T]\\d{1,2})>".toRegex()
private val headerWithMissingSpaceRegex = "#(?!\\s|#)".toRegex()
private val positionLink = "<position: (\\d+)>".toRegex()

internal fun extractLabelsFromDescription(originalPos: JosekiPosition): JosekiPosition {
  originalPos.description = originalPos.description?.let { description ->
    val labels = mutableListOf<Mark>()
    val withLabels = description
      .replace(headerWithMissingSpaceRegex, "# ")
      .replace(customMarkRegex) { match ->
        val (label, coordinate) = match.destructured
        labels.add(Mark(RulesManager.coordinateToCell(coordinate), label, PlayCategory.LABEL))
        "**$label**"
      }
    originalPos.labels = labels
    withLabels.replace(positionLink) { match ->
      "[Position ${match.groupValues[1]}](${match.groupValues[1]})"
    }
  }
  return originalPos
}
