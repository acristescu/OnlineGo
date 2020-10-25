package io.zenandroid.onlinego.utils.moshiadapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import io.zenandroid.onlinego.data.model.katago.Response
import io.zenandroid.onlinego.data.model.katago.ResponseAbreviatedJSON

class ResponseBriefMoshiAdapter {
    @ToJson
    fun toJson(response: Response): ResponseAbreviatedJSON {
        return ResponseAbreviatedJSON(
                response.rootInfo,
                response.ownership?.map { (it*100).toInt()/100f }
        )
    }

    @FromJson
    fun fromJson(json: ResponseAbreviatedJSON): Response? {
        return Response(
                id = "",
                turnNumber = 0,
                moveInfos = emptyList(),
                rootInfo =  json.rootInfo,
                policy = null,
                ownership = json.ownership
        )
    }
}