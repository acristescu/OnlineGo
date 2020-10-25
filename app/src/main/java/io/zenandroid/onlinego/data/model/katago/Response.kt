package io.zenandroid.onlinego.data.model.katago

data class Response (
        val id: String,
        val turnNumber: Int,
        val moveInfos: List<MoveInfo>,
        val rootInfo: RootInfo,
        val policy: List<Float>?,
        val ownership: List<Float>? = null
)

data class MoveInfo (
        val move: String,
        val visits: Int,
        val winrate: Float,
        val scoreStdev: Float,
        val scoreLead: Float,
        val scoreSelfplay: Float,
        val prior: Float,
        val utility: Float,
        val lcb: Float,
        val utilityLcb: Float,
        val order: Int,
        val pv: List<String>,
        val pvVisits: Int?,
        val ownership: List<Float>? = null
)

data class RootInfo (
        val scoreLead: Float? = null,
        val scoreSelfplay: Float? = null,
        val scoreStdev: Float? = null,
        val utility: Float? = null,
        val visits: Int? = null,
        val winrate: Float? = null
)

data class ResponseAbreviatedJSON (
        val rootInfo: RootInfo,
        val ownership: List<Float>? = null
)