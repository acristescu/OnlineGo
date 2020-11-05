package io.zenandroid.onlinego.data.model.ogs

data class NetPing (
        val client: Long,
        val drift: Long,
        val latency: Long
)