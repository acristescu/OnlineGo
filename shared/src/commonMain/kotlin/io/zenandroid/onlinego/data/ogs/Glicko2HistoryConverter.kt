package io.zenandroid.onlinego.data.ogs

import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.Converter
import de.jensklingenberg.ktorfit.converter.KtorfitResult
import de.jensklingenberg.ktorfit.converter.TypeData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.zenandroid.onlinego.data.model.ogs.Glicko2History
import io.zenandroid.onlinego.data.model.ogs.Glicko2HistoryItem

private const val COLUMN_COUNT = 14
private const val INITIAL_RATING_GAME_ID = 0L

object Glicko2HistoryConverterFactory : Converter.Factory {
  override fun suspendResponseConverter(
    typeData: TypeData,
    ktorfit: Ktorfit,
  ): Converter.SuspendResponseConverter<HttpResponse, *>? =
    if (typeData.typeInfo.type == Glicko2History::class) Glicko2HistoryConverter else null
}

private object Glicko2HistoryConverter :
  Converter.SuspendResponseConverter<HttpResponse, Glicko2History> {
  override suspend fun convert(result: KtorfitResult): Glicko2History = when (result) {
    is KtorfitResult.Failure -> throw result.throwable
    is KtorfitResult.Success -> parseGlicko2History(result.response.bodyAsText())
  }
}

internal fun parseGlicko2History(body: String) = Glicko2History(
  body.lineSequence()
    .drop(1)
    .mapNotNull { it.toGlicko2HistoryItemOrNull() }
    .filter { it.gameId != INITIAL_RATING_GAME_ID }
    .toList()
)

private fun String.toGlicko2HistoryItemOrNull(): Glicko2HistoryItem? {
  val tokens = split('\t')
  if (tokens.size != COLUMN_COUNT) return null
  val field = tokens.listIterator()
  return Glicko2HistoryItem(
    ended = field.next().toLong(),
    gameId = field.next().toLong(),
    playedBlack = field.next() == "1",
    handicap = field.next().toInt(),
    rating = field.next().toFloat(),
    deviation = field.next().toFloat(),
    volatility = field.next().toFloat(),
    opponentId = field.next().toLong(),
    opponentRating = field.next().toFloat(),
    opponentDeviation = field.next().toFloat(),
    won = field.next() == "1",
    extra = field.next(),
    annulled = field.next() == "1",
    result = field.next()
  )
}
