package io.zenandroid.onlinego.utils

import io.zenandroid.onlinego.data.model.ogs.Glicko2History
import io.zenandroid.onlinego.data.model.ogs.Glicko2HistoryItem
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class CustomConverterFactory : Converter.Factory() {

    override fun responseBodyConverter(type: Type, annotations: Array<Annotation>, retrofit: Retrofit): Converter<ResponseBody, *>? {
        return if(type == Glicko2History::class.java) {
            Glicko2HistoryConverter
        } else {
            null
        }
    }
}

private object Glicko2HistoryConverter  : Converter <ResponseBody, Glicko2History>{
    override fun convert(value: ResponseBody): Glicko2History? {
        val lines = value.charStream().readLines()
        val gameHistory = lines
                .drop(1) // drop headers
                .dropLast(2) // drop empty line at the end + initial rating
                .map {
                    val tokens = it
                            .split('\t')
                            .listIterator()
                    Glicko2HistoryItem(
                            ended = tokens.next().toLong(),
                            gameId = tokens.next().toLong(),
                            playedBlack = tokens.next() == "1",
                            handicap = tokens.next().toInt(),
                            rating = tokens.next().toFloat(),
                            deviation = tokens.next().toFloat(),
                            volatility = tokens.next().toFloat(),
                            opponentId = tokens.next().toLong(),
                            opponentRating = tokens.next().toFloat(),
                            opponentDeviation = tokens.next().toFloat(),
                            won = tokens.next() == "1",
                            extra = tokens.next(),
                            annulled = tokens.next() == "1",
                            result = tokens.next()
                    )
                }
        return Glicko2History(gameHistory)
    }

}
